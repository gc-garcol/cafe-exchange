package gc.garcol.exchangecore.ringbuffer;

import java.nio.ByteBuffer;

/**
 * @author thaivc
 * @since 2024
 */
public interface Producer
{
    boolean publish(int messageTypeId, ByteBuffer message, int length);
}
