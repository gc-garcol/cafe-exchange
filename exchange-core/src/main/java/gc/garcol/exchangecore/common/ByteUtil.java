package gc.garcol.exchangecore.common;

import java.nio.ByteBuffer;

/**
 * @author thaivc
 * @since 2024
 */
public class ByteUtil
{
    public static void eraseByteBuffer(ByteBuffer buffer)
    {
        int length = buffer.capacity();
        byte[] zeros = new byte[Math.min(length, 1024)];  // Small zero-filled array
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
