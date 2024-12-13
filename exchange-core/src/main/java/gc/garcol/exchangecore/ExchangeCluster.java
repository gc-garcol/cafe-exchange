package gc.garcol.exchangecore;

import gc.garcol.exchange.proto.ClusterPayloadProto;
import gc.garcol.exchangecore.common.ClusterGlobal;
import gc.garcol.exchangecore.common.Env;
import gc.garcol.exchangecore.ringbuffer.ManyToManyRingBuffer;
import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.SleepingIdleStrategy;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.ringbuffer.OneToOneRingBuffer;
import org.agrona.concurrent.ringbuffer.RingBufferDescriptor;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author thaivc
 * @since 2024
 */
@Slf4j
public class ExchangeCluster
{
    ExchangeClusterState state;

    AtomicReference<UUID> currentLeader = new AtomicReference<>();

    AtomicBuffer requestAcceptorBuffer;

    ManyToManyRingBuffer requestRingBuffer;
    OneToOneRingBuffer responseRingBuffer;
    OneToOneRingBuffer heartBeatInboundRingBuffer;
    OneToOneRingBuffer relayLogInboundRingBuffer;

    AgentDomainMessageHandler domainLogicConsumer;
    AgentRunner domainLogicRunner;

    AgentClientReply clientReplyAgent;
    AgentRunner clientReplyRunner;

    public ExchangeCluster()
    {
        requestAcceptorBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect((1 << Env.BUFFER_SIZE_REQUEST_ACCEPTOR_POW) + RingBufferDescriptor.TRAILER_LENGTH));

        var commandsOutboundBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect((1 << Env.BUFFER_SIZE_RESPONSE_POW) + RingBufferDescriptor.TRAILER_LENGTH));
        var heartBeatInboundBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect((1 << Env.BUFFER_SIZE_HEARTBEAT_POW) + RingBufferDescriptor.TRAILER_LENGTH));
        var relayLogInboundBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect((1 << Env.BUFFER_SIZE_REPLAY_LOG_POW) + RingBufferDescriptor.TRAILER_LENGTH));

        responseRingBuffer = new OneToOneRingBuffer(commandsOutboundBuffer);
        heartBeatInboundRingBuffer = new OneToOneRingBuffer(heartBeatInboundBuffer);
        relayLogInboundRingBuffer = new OneToOneRingBuffer(relayLogInboundBuffer);

        domainLogicConsumer = new AgentDomainMessageHandler(this);
        domainLogicRunner = new AgentRunner(
            new SleepingIdleStrategy(10),
            error -> log.error("Domain logic error", error),
            null,
            domainLogicConsumer
        );

        clientReplyAgent = new AgentClientReply(this);
        clientReplyRunner = new AgentRunner(
            new SleepingIdleStrategy(),
            error -> log.error("Client reply error", error),
            null,
            clientReplyAgent
        );
    }

    public void onStart()
    {
        transitionToFollower(null);
        AgentRunner.startOnThread(domainLogicRunner);
        AgentRunner.startOnThread(clientReplyRunner);
    }

    public boolean enqueueRequest(UUID sender, ClusterPayloadProto.Request request)
    {
        return this.state.enqueueRequest(sender, request);
    }

    public boolean enqueueResponse(UUID sender, long responseId)
    {
        int uuidSize = Long.BYTES * 2;
        int claimIndex = responseRingBuffer.tryClaim(1, uuidSize * 2);
        if (claimIndex <= 0)
        {
            return false;
        }

        final var buffer = responseRingBuffer.buffer();
        buffer.putLong(claimIndex, sender.getMostSignificantBits());
        buffer.putLong(claimIndex + Long.BYTES, sender.getLeastSignificantBits());
        buffer.putLong(claimIndex + Long.BYTES * 2, responseId);
        responseRingBuffer.commit(claimIndex);
        return true;
    }

    void enqueueHeartBeat(UUID leaderId)
    {
        int claimIndex = heartBeatInboundRingBuffer.tryClaim(1, Long.BYTES * 2);
        if (claimIndex <= 0)
        {
            return;
        }
        final var buffer = heartBeatInboundRingBuffer.buffer();
        buffer.putLong(claimIndex, leaderId.getMostSignificantBits());
        buffer.putLong(claimIndex + Long.BYTES, leaderId.getLeastSignificantBits());
        heartBeatInboundRingBuffer.commit(claimIndex);
    }

    void transitionToFollower(UUID leaderNode)
    {
        AgentDomainMessageHandler.IS_RUNNING.set(false);
        log.info("Transition to follower");
        if (state != null)
        {
            state.stop();
        }
        currentLeader.set(leaderNode);
        state = new ExchangeClusterStateFollower(this);
        state.start();
        AgentDomainMessageHandler.IS_RUNNING.set(true);
    }

    void transitionToLeader()
    {
        AgentDomainMessageHandler.IS_RUNNING.set(false);
        log.info("Transition to leader {}", ClusterGlobal.NODE_ID);
        if (state != null)
        {
            state.stop();
        }
        currentLeader.set(ClusterGlobal.NODE_ID);
        state = new ExchangeClusterStateLeader(this);
        state.start();
        AgentDomainMessageHandler.IS_RUNNING.set(true);
    }

    public void stopAll()
    {
        domainLogicRunner.close();
        clientReplyRunner.close();
        domainLogicRunner = null; // decrease ref-count
        clientReplyRunner = null; // decrease ref-count
        if (state != null)
        {
            state.stop();
        }
    }

}
