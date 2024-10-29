package gc.garcol.exchangecore;

import gc.garcol.exchange.proto.ClusterPayloadProto;
import gc.garcol.exchange.proto.ClusterServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author thaivc
 * @since 2024
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public class NetworkClusterService extends ClusterServiceGrpc.ClusterServiceImplBase
{

    final Map<UUID, StreamObserver<ClusterPayloadProto.Response>> repliers = new ConcurrentHashMap<>();
    final ExchangeCluster exchangeCluster;

    @Override
    public StreamObserver<ClusterPayloadProto.Request> send(final StreamObserver<ClusterPayloadProto.Response> responseObserver)
    {
        var replyChannel = UUID.randomUUID();
        repliers.put(replyChannel, responseObserver);

        return new NetworkClusterRequestStream(exchangeCluster, replyChannel, responseObserver, this);
    }
}
