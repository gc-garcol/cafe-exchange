package gc.garcol.exchangecore;

import gc.garcol.exchange.proto.ClusterPayloadProto;
import gc.garcol.exchangecore.common.ResponseCode;
import gc.garcol.exchangecore.common.StatusCode;
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
        boolean enqueueSuccess = exchangeCluster.state.enqueueRequest(replyChannel, request);
        if (!enqueueSuccess)
        {
            responseObserver.onNext(ClusterPayloadProto.Response.newBuilder()
                .setCommonResponse(ClusterPayloadProto.CommonResponse.newBuilder()
                    .setCode(ResponseCode.CLUSTER_BUSY.code)
                    .setStatus(StatusCode.SERVER_BUSY.code)
                    .build())
                .build());
        }
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
