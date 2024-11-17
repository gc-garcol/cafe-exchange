package gc.garcol.exchangecore.ringbuffer;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.nio.ByteBuffer;

/**
 * @author thaivc
 * @since 2024
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ByteBufferUtil
{
    public static void put(ByteBuffer buffer, int bufferIndex, byte[] src)
    {
        buffer.put(bufferIndex, src);
        buffer.position(bufferIndex + src.length);
    }
}
