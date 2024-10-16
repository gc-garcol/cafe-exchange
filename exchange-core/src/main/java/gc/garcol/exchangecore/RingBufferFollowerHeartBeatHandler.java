package gc.garcol.exchangecore;

import org.agrona.concurrent.ControlledMessageHandler;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author thaivc
 * @since 2024
 */
public class RingBufferFollowerHeartBeatHandler implements RingBufferOneToOneHandler
{
    public void handle(final ExchangeCluster exchangeCluster)
    {
        var ringBuffer = exchangeCluster.heartBeatInboundRingBuffer;

        AtomicBoolean becomeLeaderDetect = new AtomicBoolean(false);
        ringBuffer.controlledRead((messageType, buffer, offset, length) -> {
            if (becomeLeaderDetect.get())
            {
                return ControlledMessageHandler.Action.CONTINUE;
            }
            boolean tryBecomeLeaderSuccess = buffer.getByte(offset) == 1;
            if (tryBecomeLeaderSuccess)
            {
                becomeLeaderDetect.set(true);
            }
            return ControlledMessageHandler.Action.COMMIT;
        });
        if (becomeLeaderDetect.get())
        {
            exchangeCluster.transitionToLeader();
        }
    }
}
