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
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author thaivc
 * @since 2024
 */
@Slf4j
public class ExchangeCluster implements Agent
{
    ExchangeClusterState state;

    AtomicReference<String> currentLeader;

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
        transitionToFollower();
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

    void enqueueHeartBeat(boolean success)
    {
        int claimIndex = heartBeatInboundRingBuffer.tryClaim(1, 1);
        if (claimIndex <= 0)
        {
            return;
        }
        final var buffer = heartBeatInboundRingBuffer.buffer();
        buffer.putByte(claimIndex, (byte)(success ? 1 : 0));
        heartBeatInboundRingBuffer.commit(claimIndex);
    }

    void transitionToFollower()
    {
        log.info("Transition to follower");
        if (state != null)
        {
            state.stop();
        }
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
