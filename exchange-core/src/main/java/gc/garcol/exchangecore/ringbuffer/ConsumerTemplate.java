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
    public int poll()
    {
        if (ringBuffer != null)
        {
            return this.ringBuffer.controlledRead(this::onMessage)
                + this.ringBuffer.controlledRead(this::onMessage); // double read because of a bug in agrona when reading at the end of ringBuffer
        }
        else
        {
            return this.ringBufferReader.controlledRead(this::onMessage)
                + this.ringBufferReader.controlledRead(this::onMessage);
        }
    }

    @Override
    public int poll(final int limit)
    {
        int readMsg;
        if (ringBuffer != null)
        {
            readMsg = this.ringBuffer.controlledRead(this::onMessage, limit);
            if (readMsg < limit)
            {
                readMsg += this.ringBuffer.controlledRead(this::onMessage, limit - readMsg);
            }
        }
        else
        {
            readMsg = this.ringBufferReader.controlledRead(this::onMessage, limit);
            if (readMsg < limit)
            {
                readMsg += this.ringBufferReader.controlledRead(this::onMessage, limit - readMsg);
            }
        }
        return readMsg;
    }

    private ControlledMessageHandler.Action onMessage(int msgTypeId, MutableDirectBuffer buffer, int index, int length)
    {
        boolean isNextCircle = index < currentBarrier.index();
        boolean currentFlip = isNextCircle != currentBarrier.flip();
        int recordIndex = index - HEADER_LENGTH;
        if (previousBarrier != null)
        {
            if (currentFlip == previousBarrier.flip() && recordIndex > previousBarrier.index())
            {
                return ControlledMessageHandler.Action.ABORT;
            }
        }
        boolean consumedSuccess = consume(msgTypeId, buffer, index, length);
        if (!consumedSuccess)
        {
            return ControlledMessageHandler.Action.ABORT;
        }

        final int recordLength = length + RecordDescriptor.HEADER_LENGTH;
        final int alignedRecordLength = align(recordLength, ALIGNMENT);
        currentBarrier.pointer.set(new AtomicPointer.Pointer(currentFlip, recordIndex, alignedRecordLength));
        return ControlledMessageHandler.Action.COMMIT;
    }
}
