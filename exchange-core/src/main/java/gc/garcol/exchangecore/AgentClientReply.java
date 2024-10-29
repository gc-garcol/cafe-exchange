package gc.garcol.exchangecore;

import gc.garcol.exchange.proto.ClusterPayloadProto;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
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
        UUID sender = new UUID(buffer.getLong(index), buffer.getLong(index + Long.BYTES));
        Optional.ofNullable(networkClusterService.repliers.get(sender))
            .ifPresent(responseStream -> {
                var response = parse(buffer, index + Long.BYTES * 2, length - Long.BYTES * 2);
                responseStream.onNext(response);
            });
        return true;
    }

    @SneakyThrows
    private ClusterPayloadProto.Response parse(final MutableDirectBuffer buffer, final int index, final int length)
    {
        cachedBuffer.clear();
        buffer.getBytes(index, cachedBuffer, length);
        cachedBuffer.flip();
        return ClusterPayloadProto.Response.parseFrom(cachedBuffer);
    }
}
