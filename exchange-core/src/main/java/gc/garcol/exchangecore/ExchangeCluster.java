package gc.garcol.exchangecore;

import gc.garcol.exchangecore.common.ClusterGlobal;
import gc.garcol.exchangecore.common.Env;
import gc.garcol.exchangecore.ringbuffer.AtomicPointer;
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
    AtomicBuffer requestBuffer;

    ManyToManyRingBuffer requestRingBuffer;
    OneToOneRingBuffer responseRingBuffer;
    OneToOneRingBuffer heartBeatInboundRingBuffer;
    OneToOneRingBuffer relayLogInboundRingBuffer;

    AgentDomainMessageHandler domainLogicConsumer;
    AgentRunner domainLogicRunner;

    public ExchangeCluster()
    {
        requestAcceptorBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect((1 << Env.BUFFER_SIZE_REQUEST_ACCEPTOR_POW) + RingBufferDescriptor.TRAILER_LENGTH));
        requestBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect((1 << Env.BUFFER_SIZE_REQUEST_POW) + RingBufferDescriptor.TRAILER_LENGTH));

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

    }

    public void onStart()
    {
        transitionToFollower(null);
        AgentRunner.startOnThread(domainLogicRunner);
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
        resetDomainLogicBarrier();
        currentLeader.set(leaderNode);
        state = new ExchangeClusterStateFollower(this);
        state.start();
        AgentDomainMessageHandler.IS_RUNNING.set(true);
    }

    void transitionToLeader()
    {
        AgentDomainMessageHandler.IS_RUNNING.set(false);
        log.info("Transition to leader");
        if (state != null)
        {
            state.stop();
        }
        resetDomainLogicBarrier();
        currentLeader.set(ClusterGlobal.NODE_ID);
        state = new ExchangeClusterStateLeader(this);
        state.start();
        AgentDomainMessageHandler.IS_RUNNING.set(true);
    }

    void resetDomainLogicBarrier()
    {
        domainLogicConsumer
            .currentBarrier()
            .pointer()
            .set(new AtomicPointer.Pointer(false, 0, 0));
    }

    public void stopAll()
    {
        domainLogicRunner.close();
        if (state != null)
        {
            state.stop();
        }
    }

}
