package gc.garcol.exchangecore;

import gc.garcol.exchangecore.ringbuffer.ConsumerTemplate;
import lombok.RequiredArgsConstructor;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.Agent;

/**
 * @author thaivc
 * @since 2024
 */
@RequiredArgsConstructor
public class AgentClientReply extends ConsumerTemplate implements Agent
{

    private final ExchangeCluster exchangeCluster;

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
        var claimIndex = exchangeCluster.commandsOutboundRingBuffer.tryClaim(1, length);
        if (claimIndex <= 0)
        {
            return false;
        }
        byte[] bytes = new byte[length];
        buffer.getBytes(index, bytes);
        exchangeCluster.commandsOutboundRingBuffer.buffer().putBytes(claimIndex, bytes);
        exchangeCluster.commandsOutboundRingBuffer.commit(claimIndex);
        return true;
    }
}
