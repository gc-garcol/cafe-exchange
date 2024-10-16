package gc.garcol.exchangecore;

import gc.garcol.exchangecore.common.Env;
import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.ringbuffer.OneToOneRingBuffer;
import org.agrona.concurrent.ringbuffer.RingBufferDescriptor;

import java.nio.ByteBuffer;

/**
 * @author thaivc
 * @since 2024
 */
@Slf4j
public class ExchangeCluster implements Agent
{
    final RingBufferOneToOneHandler heartBeatFollowerHandler;
    final RingBufferOneToOneHandler heartBeatLeaderHandler;
    ExchangeClusterState state;
    OneToOneRingBuffer commandsInboundRingBuffer;
    OneToOneRingBuffer commandsOutboundRingBuffer;
    OneToOneRingBuffer heartBeatInboundRingBuffer;
    OneToOneRingBuffer relayLogInboundRingBuffer;

    public ExchangeCluster(
        RingBufferOneToOneHandler heartBeatFollowerHandler,
        RingBufferOneToOneHandler heartBeatLeaderHandler
    )
    {
        var commandInboundBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect((1 << Env.BUFFER_SIZE_COMMAND_INBOUND_POW) + RingBufferDescriptor.TRAILER_LENGTH));
        var commandsOutboundBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect((1 << Env.BUFFER_SIZE_COMMAND_OUTBOUND_POW) + RingBufferDescriptor.TRAILER_LENGTH));
        var heartBeatInboundBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect((1 << Env.BUFFER_SIZE_HEARTBEAT_POW) + RingBufferDescriptor.TRAILER_LENGTH));
        var relayLogInboundBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect((1 << Env.BUFFER_SIZE_REPLAY_LOG_POW) + RingBufferDescriptor.TRAILER_LENGTH));

        commandsInboundRingBuffer = new OneToOneRingBuffer(commandInboundBuffer);
        commandsOutboundRingBuffer = new OneToOneRingBuffer(commandsOutboundBuffer);
        heartBeatInboundRingBuffer = new OneToOneRingBuffer(heartBeatInboundBuffer);
        relayLogInboundRingBuffer = new OneToOneRingBuffer(relayLogInboundBuffer);

        this.heartBeatFollowerHandler = heartBeatFollowerHandler;
        this.heartBeatLeaderHandler = heartBeatLeaderHandler;
    }

    public void onStart()
    {
        transitionToFollower();
    }

    public int doWork() throws Exception
    {
        if (state instanceof ExchangeClusterStateFollower)
        {
            heartBeatFollowerHandler.handle(this);
        }
        else if (state instanceof ExchangeClusterStateLeader)
        {
            heartBeatLeaderHandler.handle(this);
        }
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
