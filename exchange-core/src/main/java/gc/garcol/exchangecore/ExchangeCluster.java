package gc.garcol.exchangecore;

import gc.garcol.exchangecore.common.Env;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.ringbuffer.OneToOneRingBuffer;
import org.agrona.concurrent.ringbuffer.RingBufferDescriptor;

import java.nio.ByteBuffer;

/**
 * @author thaivc
 * @since 2024
 */
public class ExchangeCluster
{
    ExchangeClusterState state;

    OneToOneRingBuffer commandsInboundRingBuffer;
    OneToOneRingBuffer commandsOutboundRingBuffer;

    OneToOneRingBuffer heartBeatInboundRingBuffer;
    OneToOneRingBuffer relayLogInboundRingBuffer;

    public ExchangeCluster()
    {
        var commandInboundBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect((1 << Env.BUFFER_SIZE_COMMAND_INBOUND_POW) + RingBufferDescriptor.TRAILER_LENGTH));
        var commandsOutboundBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect((1 << Env.BUFFER_SIZE_COMMAND_OUTBOUND_POW) + RingBufferDescriptor.TRAILER_LENGTH));
        var heartBeatInboundBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect((1 << Env.BUFFER_SIZE_HEARTBEAT_POW) + RingBufferDescriptor.TRAILER_LENGTH));
        var relayLogInboundBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect((1 << Env.BUFFER_SIZE_REPLAY_LOG_POW) + RingBufferDescriptor.TRAILER_LENGTH));

        commandsInboundRingBuffer = new OneToOneRingBuffer(commandInboundBuffer);
        commandsOutboundRingBuffer = new OneToOneRingBuffer(commandsOutboundBuffer);
        heartBeatInboundRingBuffer = new OneToOneRingBuffer(heartBeatInboundBuffer);
        relayLogInboundRingBuffer = new OneToOneRingBuffer(relayLogInboundBuffer);
    }

    public void onStart()
    {
        transitionToFollower();
    }

    void transitionToFollower()
    {
        if (state != null)
        {
            state.stop();
        }
        state = new ExchangeClusterStateFollower(this);
    }

    void transitionToLeader()
    {
        if (state != null)
        {
            state.stop();
        }
        state = new ExchangeClusterStateLeader(this);
    }

}
