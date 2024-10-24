package gc.garcol.exchangecore.ringbuffer;

import lombok.RequiredArgsConstructor;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.ControlledMessageHandler;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;

import java.util.UUID;

/**
 * ManyToManyRingBuffer = pipeline(ManyToOneRingBuffer -> OneToManyRingBuffer)
 *
 * @author thaivc
 * @since 2024
 */
@RequiredArgsConstructor
public class ManyToManyRingBuffer
{
    private final ManyToOneRingBuffer inboundRingBuffer;
    private final OneToManyRingBuffer oneToManyRingBuffer;

    public boolean publishMessage(int messageType, UUID sender, byte[] message)
    {
        int claimIndex = inboundRingBuffer.tryClaim(messageType, message.length + Long.BYTES * 2);
        if (claimIndex <= 0)
        {
            return false;
        }
        inboundRingBuffer.buffer().putLong(claimIndex, sender.getMostSignificantBits());
        inboundRingBuffer.buffer().putLong(claimIndex + Long.BYTES, sender.getLeastSignificantBits());
        inboundRingBuffer.buffer().putBytes(claimIndex + Long.BYTES * 2, message);
        inboundRingBuffer.commit(claimIndex);
        return true;
    }

    public void transfer()
    {
        inboundRingBuffer.controlledRead((int msgTypeId, MutableDirectBuffer buffer, int index, int length) -> {
            byte[] message = new byte[length];
            buffer.getBytes(index + Long.BYTES * 2, message);
            boolean success = oneToManyRingBuffer.producer().publish(1, message);
            return success ? ControlledMessageHandler.Action.CONTINUE : ControlledMessageHandler.Action.ABORT;
        });
    }
}
