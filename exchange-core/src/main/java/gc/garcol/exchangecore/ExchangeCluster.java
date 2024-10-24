package gc.garcol.exchangecore;

import gc.garcol.exchangecore.common.Env;
import gc.garcol.exchangecore.ringbuffer.ManyToManyRingBuffer;
import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.AtomicBuffer;
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
public class ExchangeCluster implements Agent
{
    ExchangeClusterState state;

    AtomicReference<UUID> currentLeader = new AtomicReference<>();

    AtomicBuffer requestAcceptorBuffer;
    AtomicBuffer requestBuffer;

    ManyToManyRingBuffer requestRingBuffer;
    OneToOneRingBuffer responseRingBuffer;
    OneToOneRingBuffer heartBeatInboundRingBuffer;
    OneToOneRingBuffer relayLogInboundRingBuffer;

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
    }

    public void onStart()
    {
        transitionToFollower(null);
    }

    /**
     * All inbound messages that change the state must be handled in these methods
     */
    public int doWork() throws Exception
    {
        this.state.handleHeartBeatEvent();

        return 0;
    }

    public String roleName()
    {
        return "Exchange cluster runner";
    }

    public void onClose()
    {
        Agent.super.onClose();
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
        log.info("Transition to follower");
        if (state != null)
        {
            state.stop();
        }
        currentLeader.set(leaderNode);
        state = new ExchangeClusterStateFollower(this);
        state.start();
    }

    void transitionToLeader()
    {
        log.info("Transition to leader");
        if (state != null)
        {
            state.stop();
        }
        currentLeader.set(ClusterGlobal.NODE_ID);
        state = new ExchangeClusterStateLeader(this);
        state.start();
    }

    public void stopAll()
    {
        if (state != null)
        {
            state.stop();
        }
    }

}
