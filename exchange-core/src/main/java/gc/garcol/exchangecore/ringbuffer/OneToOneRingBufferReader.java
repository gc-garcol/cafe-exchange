package gc.garcol.exchangecore.ringbuffer;

import lombok.RequiredArgsConstructor;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.ControlledMessageHandler;
import org.agrona.concurrent.MessageHandler;
import org.agrona.concurrent.ringbuffer.OneToOneRingBuffer;

import static org.agrona.BitUtil.align;
import static org.agrona.concurrent.ControlledMessageHandler.Action.*;
import static org.agrona.concurrent.ringbuffer.RecordDescriptor.*;
import static org.agrona.concurrent.ringbuffer.RingBuffer.PADDING_MSG_TYPE_ID;

/**
 * @author thaivc
 * @since 2024
 */
@RequiredArgsConstructor
public class OneToOneRingBufferReader
{
    final AtomicBuffer buffer;
    final int capacity;
    private int headPositionIndex;

    /**
     * {@link OneToOneRingBuffer#read(MessageHandler)}
     */
    public int controlledRead(final ControlledMessageHandler handler)
    {
        return controlledRead(handler, Integer.MAX_VALUE);
    }

    /**
     * {@link OneToOneRingBuffer#read(MessageHandler, int)}
     */
    public int controlledRead(final ControlledMessageHandler handler, final int messageCountLimit)
    {
        int messagesRead = 0;

        final AtomicBuffer buffer = this.buffer;
        long head = this.headPositionIndex;

        int bytesRead = 0;

        final int capacity = this.capacity;
        int headIndex = (int)head & (capacity - 1);
        final int contiguousBlockLength = capacity - headIndex;

        try
        {
            while ((bytesRead < contiguousBlockLength) && (messagesRead < messageCountLimit))
            {
                final int recordIndex = headIndex + bytesRead;
                final int recordLength = buffer.getIntVolatile(lengthOffset(recordIndex));
                if (recordLength <= 0)
                {
                    break;
                }

                final int alignedLength = align(recordLength, ALIGNMENT);
                bytesRead += alignedLength;

                final int messageTypeId = buffer.getInt(typeOffset(recordIndex));
                if (PADDING_MSG_TYPE_ID == messageTypeId)
                {
                    continue;
                }

                final ControlledMessageHandler.Action action = handler.onMessage(
                    messageTypeId, buffer, recordIndex + HEADER_LENGTH, recordLength - HEADER_LENGTH);

                if (ABORT == action)
                {
                    bytesRead -= alignedLength;
                    break;
                }

                ++messagesRead;

                if (BREAK == action)
                {
                    break;
                }
                if (COMMIT == action)
                {
                    headPositionIndex = (int)head + bytesRead;
                    headIndex += bytesRead;
                    head += bytesRead;
                    bytesRead = 0;
                }
            }
        }
        finally
        {
            if (bytesRead > 0)
            {
                headPositionIndex = (int)head + bytesRead;
            }
        }

        return messagesRead;
    }
}
