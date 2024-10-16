package gc.garcol.exchangecore;

import lombok.RequiredArgsConstructor;
import org.agrona.concurrent.ControlledMessageHandler;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author thaivc
 * @since 2024
 */
@RequiredArgsConstructor
public class RingBufferHeartBeatLeaderHandler implements RingBufferOneToOneHandler
{

    public void handle(final ExchangeCluster exchangeCluster)
    {
        var ringBuffer = exchangeCluster.heartBeatInboundRingBuffer;

        AtomicBoolean keepLeaderState = new AtomicBoolean(true);
        ringBuffer.controlledRead((messageType, buffer, offset, length) -> {
            if (!keepLeaderState.get())
            {
                return ControlledMessageHandler.Action.CONTINUE;
            }
            boolean keepLeaderStateSuccess = buffer.getByte(offset) == 1;
            if (!keepLeaderStateSuccess)
            {
                keepLeaderState.set(false);
            }
            return ControlledMessageHandler.Action.COMMIT;
        });
        if (!keepLeaderState.get())
        {
            exchangeCluster.transitionToFollower();
        }
    }
}
