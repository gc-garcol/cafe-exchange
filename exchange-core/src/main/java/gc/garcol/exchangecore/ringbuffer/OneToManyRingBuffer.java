package gc.garcol.exchangecore.ringbuffer;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * @author thaivc
 * @since 2024
 */
public class OneToManyRingBuffer
{
    private final UnsafeBuffer unsafeBuffer;
    private final AtomicLong producerPosition = new AtomicLong();
    private final AtomicLongArray consumerPositions;

    private final int capacity;
    private final int maxMsgLength;
    private final int lastConsumerIndex;

    public static final int HEADER_LENGTH = Integer.BYTES * 2; // length, type

    /**
     * Alignment as a multiple of bytes for each record.
     */
    public static final int ALIGNMENT = Long.BYTES * 8; // padding to align the record in order to prevent false sharing

    public OneToManyRingBuffer(int powSize, int consumerSize)
    {

        if (powSize < 10)
        {
            throw new IllegalArgumentException("Ring buffer size must be greater than 1024");
        }

        if (consumerSize < 1)
        {
            throw new IllegalArgumentException("Consumer size must be greater than 0");
        }

        capacity = 1 << powSize;
        unsafeBuffer = new UnsafeBuffer(capacity);
        consumerPositions = new AtomicLongArray(consumerSize);
        lastConsumerIndex = consumerSize - 1;

        maxMsgLength = capacity >> 3;
    }

    /**
     * Write a message to the ring buffer
     *
     * @param msgTypeId
     * @param message   the message to write, the limit must be equals to the message length
     * @return the message was written successfully or not
     */
    public boolean write(int msgTypeId, ByteBuffer message)
    {
        int msgLength = message.limit();
        checkMsgLength(msgLength);

        // [1] happen-before guarantee for reads
        long currentProducerPosition = producerPosition.get();
        long firstConsumerPosition = consumerPositions.get(0);
        long lastConsumerPosition = consumerPositions.get(lastConsumerIndex);

        int currentProducerOffset = offset(currentProducerPosition);
        boolean currentProducerFlip = flip(currentProducerPosition);
        int firstConsumerOffset = offset(firstConsumerPosition);
        boolean firstConsumerFlip = flip(firstConsumerPosition);
        int lastConsumerOffset = offset(lastConsumerPosition);
        boolean lastConsumerFlip = flip(lastConsumerPosition);

        final int recordLength = msgLength + HEADER_LENGTH;
        final int alignedRecordLength = BitUtil.align(recordLength, ALIGNMENT);

        final int expectedEndOffsetOfRecord = currentProducerOffset + alignedRecordLength - 1;

        boolean sameCircleWithFirstConsumer = sameCircle(currentProducerFlip, firstConsumerFlip);
        boolean sameCircleWithLastConsumer = sameCircle(currentProducerFlip, lastConsumerFlip);

        int realStartOfRecord = currentProducerOffset;

        boolean shouldFlip = false;

        // C3 . . C2 . . C1 . . P . . x
        if (sameCircleWithFirstConsumer && sameCircleWithLastConsumer)
        {
            //                R E C O R D
            // C3 . C2 . C1 . P . . x
            if (expectedEndOffsetOfRecord >= capacity)
            {
                // R E C O R D
                // . . . . . . C3 . C2 . C1 . . P . . x
                if (lastConsumerOffset > alignedRecordLength - 1)
                {
                    realStartOfRecord = 0; // jump to the beginning of the buffer
                    shouldFlip = true;
                }
                else
                {
                    return false;
                }
            }
            // else keeping the realStartOfRecord = producerOffset.get();

            //                      R E C O R D
            // C3 . . C2 . . C1 . . P . . . . x
        }
        else if (!sameCircleWithLastConsumer) // !sameCircleWithFirstConsumer, the producer is on the next circle which is higher than last consumer
        {
            //             R  E  C  O  R  D
            // .  C1  .  . P  .  .  .  C3 .  .  . C2  .  .  x
            if (expectedEndOffsetOfRecord >= lastConsumerOffset)
            {
                return false;
            }

            // else keeping the realStartOfRecord = producerOffset.get();

            //             R  E  C  O  R  D
            // .  C1  .  . P  .  .  .  .  .  C3 .  .  . C2  .  .  x
        }

        UnsafeBuffer buffer = this.unsafeBuffer;

        int nextProducerOffset = (realStartOfRecord + alignedRecordLength) % capacity; // maybe nextProducerOffset == 0

        // when [2] happened, the [2] ensures that the these instructions are synchronized into main memory as well
        buffer.putBytes(realStartOfRecord + HEADER_LENGTH, message, 0, msgLength);
        buffer.putInt(realStartOfRecord, msgLength);
        buffer.putInt(realStartOfRecord + Integer.BYTES, msgTypeId);

        shouldFlip |= nextProducerOffset == 0;
        boolean newProducerFlip = shouldFlip != currentProducerFlip;
        long newProducerPosition = position(nextProducerOffset, newProducerFlip);
        // [2]: happen-before guarantee for writes
        producerPosition.set(newProducerPosition);

        return true;
    }

    /**
     * Read message from the ring buffer, apply for consumeIndex-th consumer
     */
    public int read(int consumerIndex, final MessageHandler handler)
    {
        return read(consumerIndex, handler, Integer.MAX_VALUE);
    }

    /**
     * Read message from the ring buffer, apply for consumeIndex-th consumer
     *
     * @param consumerIndex the index of the consumer
     * @param handler       the handler to process the message
     * @param limit         the maximum number of messages to read
     * @return the number of messages read
     */
    public int read(int consumerIndex, final MessageHandler handler, int limit)
    {
        for (int i = 0; i < limit; i++)
        {
            if (!readOne(consumerIndex, handler))
            {
                return i;
            }
        }
        return limit;
    }

    /**
     * Read one message from the ring buffer, apply for consumeIndex-th consumer
     *
     * @param consumerIndex the index of the consumer
     * @param handler       the handler to process the message
     * @return true if a message was read otherwise false
     */
    public boolean readOne(int consumerIndex, final MessageHandler handler)
    {
        // [1] happen-before guarantee for reads
        long barrierPosition = consumerIndex == 0 ? producerPosition.get() : consumerPositions.get(consumerIndex - 1);

        int previousBarrier = offset(barrierPosition);
        boolean previousFlip = flip(barrierPosition);

        long currentConsumerPosition = consumerPositions.get(consumerIndex);
        int currentConsumerOffset = offset(currentConsumerPosition);
        boolean currentConsumerFlip = flip(currentConsumerPosition);

        if (sameCircle(currentConsumerFlip, previousFlip))
        {

            // . . . . . P . . . . . x
            //           C1

            // or

            // . . . . . C1 . . . . . x
            //           C2
            if (currentConsumerOffset >= previousBarrier)
            {
                return false;
            }

            // R  E C O R D
            // C  . . . . . P . . . . . x

            // or

            // R  E C O R D
            // C1 . . . . . C2 . . . . . x
        }

        // when [1] happened, the [1] ensures that the these instructions are loaded from the main memory as well
        int messageLength = unsafeBuffer.getInt(currentConsumerOffset);

        if (messageLength == 0)
        {

            //  . . . . . . C2 0 0 0
            //              C1
            //              P
            if (sameCircle(currentConsumerFlip, previousFlip))
            {
                return false;
            }

            // R  E C O R D
            // C1 . . . . . P . . . . .C2 0 0 0

            // or

            //
            // P . . . . . . C1 0 0 0

            // when [2] happened, the [2] ensures that the these instructions are synchronized into main memory as well
            unsafeBuffer.clearBytes(currentConsumerOffset, capacity - 1);

            int nextConsumerOffset = 0;
            boolean newFlip = !currentConsumerFlip;
            long newConsumerPosition = position(nextConsumerOffset, newFlip);

            // [2] happen-before guarantee for writes
            consumerPositions.set(consumerIndex, newConsumerPosition);
            return readOne(consumerIndex, handler);
        }

        int messageTypeId = unsafeBuffer.getInt(currentConsumerOffset + Integer.BYTES);

        boolean consumeSuccess = handler.onMessage(messageTypeId, unsafeBuffer, currentConsumerOffset + HEADER_LENGTH, messageLength);

        if (!consumeSuccess)
        {
            return false;
        }

        int recordLength = messageLength + HEADER_LENGTH;
        int alignedRecordLength = BitUtil.align(recordLength, ALIGNMENT);
        int endRecordOffset = currentConsumerOffset + alignedRecordLength - 1;

        if (consumerIndex == lastConsumerIndex)
        {
            // when [3] happened, the [3] ensures that the these instructions are synchronized into main memory as well
            unsafeBuffer.clearBytes(currentConsumerOffset, endRecordOffset);
        }

        int nextConsumerOffset = (endRecordOffset + 1) % capacity; // nextConsumerOffset == 0 if the endRecordOffset is at the end of the buffer
        boolean shouldFlip = nextConsumerOffset == 0;
        boolean newFlip = shouldFlip != currentConsumerFlip;

        long newConsumerPosition = position(nextConsumerOffset, newFlip);

        // [3] happen-before guarantee for writes
        consumerPositions.set(consumerIndex, newConsumerPosition);

        return true;
    }

    private void checkMsgLength(final int length)
    {
        if (length < 0)
        {
            throw new IllegalArgumentException("invalid message length=" + length);
        }
        else if (length > maxMsgLength)
        {
            throw new IllegalArgumentException(
                "encoded message exceeds maxMsgLength=" + maxMsgLength + ", length=" + length);
        }
    }

    private boolean sameCircle(boolean firstFlip, boolean secondFlip)
    {
        return firstFlip == secondFlip;
    }

    private int offset(long position)
    {
        return (int)(position >> 32);
    }

    private boolean flip(long position)
    {
        return (int)(position & 0xFFFFFFFFL) == 1;
    }

    private long position(int offset, boolean flip)
    {
        return ((long)offset << 32) | (flip ? 1 : 0);
    }
}
