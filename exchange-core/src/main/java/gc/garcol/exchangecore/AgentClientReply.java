package gc.garcol.exchangecore;

import lombok.RequiredArgsConstructor;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.Agent;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.UUID;

/**
 * @author thaivc
 * @since 2024
 */
@RequiredArgsConstructor
public class AgentClientReply implements Agent
{
    final ByteBuffer cachedBuffer = ByteBuffer.allocate(1 << 10);
    final ExchangeCluster exchangeCluster;

    public int doWork() throws Exception
    {
        exchangeCluster.responseRingBuffer.read(this::consume);
        return 0;
    }

    public String roleName()
    {
        return "ClientReply";
    }

    public boolean consume(final int msgTypeId, final MutableDirectBuffer buffer, final int index, final int length)
    {
        NetworkClusterService networkClusterService = ExchangeIOC.SINGLETON.getInstance(NetworkClusterService.class);
        UUID sender = new UUID(
            buffer.getLong(index),
            buffer.getLong(index + Long.BYTES)
        );

        long responseId = buffer.getLong(index + Long.BYTES * 2);
        Optional.ofNullable(networkClusterService.repliers.get(sender))
            .ifPresent(responseStream -> {
                var response = ExchangePayloadHolder.RESPONSES.remove(responseId);
                if (response == null)
                {
                    return;
                }
                responseStream.onNext(response);
            });
        return true;
    }
}
