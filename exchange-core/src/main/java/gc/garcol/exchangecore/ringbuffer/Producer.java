package gc.garcol.exchangecore.ringbuffer;

/**
 * @author thaivc
 * @since 2024
 */
public interface Producer
{
    boolean publish(int messageTypeId, byte[] message);
}
