package gc.garcol.exchangecore.ringbuffer;


import java.nio.ByteBuffer;

import static gc.garcol.exchangecore.ringbuffer.BufferUtil.ARRAY_BASE_OFFSET;

/**
 * @author thaivc
 * @since 2024
 */
public class UnsafeBuffer
{

    private final byte[] buffer;

    public UnsafeBuffer(int initialCapacity)
    {
        buffer = new byte[initialCapacity];
    }

    public void putInt(final int index, final int value)
    {
        UnsafeHelper.UNSAFE.putInt(buffer, ARRAY_BASE_OFFSET + index, value);
    }

    public int getInt(final int index)
    {
        return UnsafeHelper.UNSAFE.getInt(buffer, ARRAY_BASE_OFFSET + index);
    }

    public void putLong(final int index, final long value)
    {
        UnsafeHelper.UNSAFE.putLong(buffer, ARRAY_BASE_OFFSET + index, value);
    }

    public long getLong(final int index)
    {
        return UnsafeHelper.UNSAFE.getLong(buffer, ARRAY_BASE_OFFSET + index);
    }

    public void putBytes(final int index, final ByteBuffer srcBuffer, final int srcIndex, final int length)
    {
        final byte[] srcByteArray = BufferUtil.array(srcBuffer);
        final long srcBaseOffset = ARRAY_BASE_OFFSET + BufferUtil.arrayOffset(srcBuffer);
        UnsafeHelper.UNSAFE.copyMemory(srcByteArray, srcBaseOffset + srcIndex, buffer, ARRAY_BASE_OFFSET + index, length);
    }

    public void getBytes(final int index, final ByteBuffer dstBuffer, final int dstOffset, final int length)
    {
        final byte[] dstByteArray = BufferUtil.array(dstBuffer);
        final long dstBaseOffset = ARRAY_BASE_OFFSET + BufferUtil.arrayOffset(dstBuffer);
        UnsafeHelper.UNSAFE.copyMemory(buffer, ARRAY_BASE_OFFSET + index, dstByteArray, dstBaseOffset + dstOffset, length);
    }

    public void clearBytes(final int fromIndex, final int toIndex)
    {
        int messageLength = toIndex - fromIndex + 1;
        UnsafeHelper.UNSAFE.setMemory(buffer, ARRAY_BASE_OFFSET + fromIndex, messageLength, (byte)0);
    }

}
