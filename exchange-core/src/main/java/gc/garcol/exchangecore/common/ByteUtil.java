package gc.garcol.exchangecore.common;

import java.nio.ByteBuffer;

/**
 * @author thaivc
 * @since 2024
 */
public class ByteUtil
{
    final static byte[] zeros = new byte[1 << 10];  // Small zero-filled array

    public static void eraseByteBuffer(ByteBuffer buffer)
    {
        int length = buffer.capacity();
        buffer.clear(); // Reset position and limit
        while (length > 0)
        {
            int chunkSize = Math.min(length, zeros.length);
            buffer.put(zeros, 0, chunkSize);  // Bulk write zeros
            length -= chunkSize;
        }
        buffer.clear();  // Reset the buffer for writing again
    }
}
