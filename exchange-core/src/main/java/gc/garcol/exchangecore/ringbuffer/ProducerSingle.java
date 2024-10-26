package gc.garcol.exchangecore.ringbuffer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.ringbuffer.OneToOneRingBuffer;
import org.agrona.concurrent.ringbuffer.RecordDescriptor;

import java.nio.ByteBuffer;

import static org.agrona.BitUtil.align;
import static org.agrona.concurrent.ringbuffer.RecordDescriptor.ALIGNMENT;
import static org.agrona.concurrent.ringbuffer.RecordDescriptor.HEADER_LENGTH;
import static org.agrona.concurrent.ringbuffer.RingBufferDescriptor.TAIL_POSITION_OFFSET;

/**
 * Single Threaded Producer
 *
 * @author thaivc
 * @since 2024
 */
@Accessors(fluent = true, chain = true)
@RequiredArgsConstructor
public class ProducerSingle implements Producer
{
    @Getter
    private final String roleName;
    @Getter
    private final AtomicPointer currentBarrier = new AtomicPointer();
    @Setter
    private OneToOneRingBuffer ringBuffer;
    @Getter
    @Setter
    private AtomicPointer lastConsumerBarrier;

    @Override
    public boolean publish(int messageTypeId, ByteBuffer message)
    {
        boolean expectedFlip = false;
        int alignedRecordLength = 0;
        if (lastConsumerBarrier != null)
        {
            /**
             * see {@link OneToOneRingBuffer#claimCapacity(AtomicBuffer, int)}
             */
            final int recordLength = message.position() + RecordDescriptor.HEADER_LENGTH;
            alignedRecordLength = align(recordLength, ALIGNMENT);
            final int tailPositionIndex = ringBuffer.capacity() + TAIL_POSITION_OFFSET;
            final int mask = ringBuffer.capacity() - 1;
            final long tail = ringBuffer.buffer().getLong(tailPositionIndex);
            final int recordIndex = (int)tail & mask;

            int expectedEndIndex = recordIndex + alignedRecordLength;
            boolean isNextCircle = expectedEndIndex < currentBarrier.index();
            expectedFlip = isNextCircle != currentBarrier.flip();

            final int lastConsumerTail = lastConsumerBarrier.index() + lastConsumerBarrier.length();
            final boolean lastConsumerFlip = lastConsumerBarrier.flip();

            if (lastConsumerFlip != expectedFlip && expectedEndIndex >= lastConsumerTail)
            {
                return false;
            }
        }

        final int claimIndex = ringBuffer.tryClaim(messageTypeId, message.position());
        if (claimIndex <= 0)
        {
            return false;
        }

        if (lastConsumerBarrier != null)
        {
            currentBarrier.pointer.set(new AtomicPointer.Pointer(expectedFlip, claimIndex - HEADER_LENGTH, alignedRecordLength));
        }
        ringBuffer.buffer().putBytes(claimIndex, message, 0, message.position());
        ringBuffer.commit(claimIndex);
        return true;
    }
}
