package gc.garcol.exchangecore.ringbuffer;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.ControlledMessageHandler;
import org.agrona.concurrent.ringbuffer.OneToOneRingBuffer;
import org.agrona.concurrent.ringbuffer.RecordDescriptor;

import static org.agrona.BitUtil.align;
import static org.agrona.concurrent.ringbuffer.RecordDescriptor.ALIGNMENT;
import static org.agrona.concurrent.ringbuffer.RecordDescriptor.HEADER_LENGTH;

/**
 * @author thaivc
 * @since 2024
 */
@Accessors(fluent = true)
public abstract class ConsumerTemplate implements Consumer
{
    @Getter
    private final AtomicPointer currentBarrier = new AtomicPointer();

    @Getter
    @Setter
    private String roleName;
    private AtomicPointer previousBarrier;

    @Getter
    @Setter
    private OneToOneRingBuffer ringBuffer;

    @Getter
    @Setter
    private OneToOneRingBufferReader ringBufferReader;

    /**
     * Make this consumer to be secondary consumer of the ring buffer.
     */
    public ConsumerTemplate handleAfter(ConsumerTemplate previousConsumer)
    {
        var sharedBuffer = previousConsumer.ringBuffer != null
            ? previousConsumer.ringBuffer.buffer() : previousConsumer.ringBufferReader.buffer;

        var capacity = previousConsumer.ringBuffer != null
            ? previousConsumer.ringBuffer.capacity() : previousConsumer.ringBufferReader.capacity;

        this.previousBarrier = previousConsumer.currentBarrier;
        this.ringBufferReader = new OneToOneRingBufferReader(sharedBuffer, capacity);
        return this;
    }

    @Override
    public void poll()
    {
        if (ringBuffer != null)
        {
            this.ringBuffer.controlledRead(this::onMessage);
            this.ringBuffer.controlledRead(this::onMessage); // double read because of a bug in agrona when reading at the end of ringBuffer
        }
        else
        {
            this.ringBufferReader.controlledRead(this::onMessage);
            this.ringBufferReader.controlledRead(this::onMessage);
        }
    }

    @Override
    public void poll(final int limit)
    {
        if (ringBuffer != null)
        {
            this.ringBuffer.controlledRead(this::onMessage, limit);
        }
        else
        {
            this.ringBufferReader.controlledRead(this::onMessage, limit);
        }
    }

    private ControlledMessageHandler.Action onMessage(int msgTypeId, MutableDirectBuffer buffer, int index, int length)
    {
        boolean isNextCircle = index < currentBarrier.index();
        long readCircle = isNextCircle ? currentBarrier.circle() + 1 : currentBarrier.circle();
        int recordIndex = index - HEADER_LENGTH;
        if (previousBarrier != null)
        {
            if (readCircle == previousBarrier.circle() && recordIndex > previousBarrier.index())
            {
                return ControlledMessageHandler.Action.ABORT;
            }
        }
        consume(msgTypeId, buffer, index, length);

        final int recordLength = length + RecordDescriptor.HEADER_LENGTH;
        final int alignedRecordLength = align(recordLength, ALIGNMENT);
        currentBarrier.pointer.set(new AtomicPointer.Pointer(readCircle, recordIndex, alignedRecordLength));
        return ControlledMessageHandler.Action.COMMIT;
    }
}
