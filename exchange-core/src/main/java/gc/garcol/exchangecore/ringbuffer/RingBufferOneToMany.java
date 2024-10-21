package gc.garcol.exchangecore.ringbuffer;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.ringbuffer.OneToOneRingBuffer;
import org.agrona.concurrent.ringbuffer.RingBufferDescriptor;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;

/**
 * @author thaivc
 * @since 2024
 */
@Getter
@Accessors(fluent = true)
public class RingBufferOneToMany
{
    private final OneToOneRingBuffer ringBuffer;
    private final ProducerSingle producer;
    private final List<ConsumerTemplate> consumers;

    public RingBufferOneToMany(int ringSizePow, List<ConsumerTemplate> consumers)
    {
        Objects.requireNonNull(consumers, "Consumers cannot be null.");
        assert !consumers.isEmpty();

        AtomicBuffer atomicBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect((1 << ringSizePow) + RingBufferDescriptor.TRAILER_LENGTH));
        this.ringBuffer = new OneToOneRingBuffer(atomicBuffer);
        this.producer = new ProducerSingle("SingleProducer");
        this.consumers = consumers;

        build();
    }

    private void build()
    {
        consumers.getFirst().ringBuffer(ringBuffer);
        for (int i = 1; i < consumers.size(); i++)
        {
            consumers.get(i).handleAfter(consumers.get(i - 1));
        }
        producer.ringBuffer(ringBuffer);
        if (consumers.size() > 1) {
            producer.lastConsumerBarrier(consumers.getLast().currentBarrier());
        }
    }
}
