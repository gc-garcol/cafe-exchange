package gc.garcol.exchangecore;

import gc.garcol.exchange.proto.ClusterPayloadProto;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * @author thaivc
 * @since 2024
 */
@Slf4j
@RequiredArgsConstructor
public class NetworkClusterRequestStream implements StreamObserver<ClusterPayloadProto.Request>
{
    final ExchangeCluster exchangeCluster;
    final UUID replyChannel;
    final StreamObserver<ClusterPayloadProto.Response> responseObserver;
    final NetworkClusterService networkClusterService;

    @Override
    public void onNext(final ClusterPayloadProto.Request request)
    {
        exchangeCluster.enqueueRequest(replyChannel, request);
    }

    @Override
    public void onError(final Throwable throwable)
    {
        log.error("{} on command streaming error {}", replyChannel, throwable.getMessage());
        networkClusterService.repliers.remove(replyChannel);
        responseObserver.onError(throwable);
    }

    @Override
    public void onCompleted()
    {
        log.info("{} on completed command streaming", replyChannel);
        networkClusterService.repliers.remove(replyChannel);
        responseObserver.onCompleted();
    }
}
