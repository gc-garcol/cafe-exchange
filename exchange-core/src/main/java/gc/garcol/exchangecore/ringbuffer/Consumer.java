package gc.garcol.exchangecore.ringbuffer;

import org.agrona.MutableDirectBuffer;

/**
 * @author thaivc
 * @since 2024
 */
public interface Consumer
{
    ConsumerTemplate handleAfter(ConsumerTemplate previousConsumer);

    int poll();

    int poll(int limit);

    boolean consume(final int msgTypeId, final MutableDirectBuffer buffer, final int index, final int length);
}
