package gc.garcol.exchangecore;

import gc.garcol.exchangecore.ringbuffer.ConsumerTemplate;
import lombok.RequiredArgsConstructor;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.Agent;

import java.nio.ByteBuffer;

/**
 * @author thaivc
 * @since 2024
 */
@RequiredArgsConstructor
public class AgentClientReply extends ConsumerTemplate implements Agent
{

    private final ExchangeCluster exchangeCluster;

    final ByteBuffer cachedBuffer = ByteBuffer.allocate(1 << 10);

    public int doWork() throws Exception
    {
        this.poll();
        return 0;
    }

    public String roleName()
    {
        return "ClientReply";
    }

    public boolean consume(final int msgTypeId, final MutableDirectBuffer buffer, final int index, final int length)
    {
        var claimIndex = exchangeCluster.responseRingBuffer.tryClaim(1, length);
        if (claimIndex <= 0)
        {
            return false;
        }
        cachedBuffer.clear();
        buffer.getBytes(index, cachedBuffer, length);
        exchangeCluster.responseRingBuffer.buffer().putBytes(claimIndex, cachedBuffer, 0, length);
        exchangeCluster.responseRingBuffer.commit(claimIndex);
        return true;
    }
}
