package gc.garcol.exchangecore.ringbuffer;

import gc.garcol.libcore.OneToManyRingBuffer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.ControlledMessageHandler;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * ManyToManyRingBuffer = pipeline(ManyToOneRingBuffer -> OneToManyRingBuffer)
 *
 * @author thaivc
 * @since 2024
 */
@Slf4j
@Accessors(fluent = true)
@RequiredArgsConstructor
public class ManyToManyRingBuffer
{
    private final ManyToOneRingBuffer inboundRingBuffer;

    @Getter
    private final OneToManyRingBuffer oneToManyRingBuffer;
    private final ByteBuffer cachedBuffer = ByteBuffer.allocate(1 << 10);

    public boolean publishMessage(int messageType, long requestId, UUID sender)
    {
        int uuidSize = Long.BYTES * 2;
        int claimIndex = inboundRingBuffer.tryClaim(messageType, uuidSize * 2);
        if (claimIndex <= 0)
        {
            return false;
        }
        inboundRingBuffer.buffer().putLong(claimIndex, sender.getMostSignificantBits());
        inboundRingBuffer.buffer().putLong(claimIndex + Long.BYTES, sender.getLeastSignificantBits());
        inboundRingBuffer.buffer().putLong(claimIndex + Long.BYTES * 2, requestId);
        inboundRingBuffer.commit(claimIndex);
        return true;
    }

    public void transfer()
    {
        inboundRingBuffer.controlledRead((int msgTypeId, MutableDirectBuffer buffer, int index, int length) -> {
            cachedBuffer.clear();
            buffer.getBytes(index, cachedBuffer, length);
            cachedBuffer.position(length);
            cachedBuffer.flip();
            boolean success = oneToManyRingBuffer.write(msgTypeId, cachedBuffer);
            return success ? ControlledMessageHandler.Action.CONTINUE : ControlledMessageHandler.Action.ABORT;
        });
    }
}
