package gc.garcol.exchangeclient.domain;

import gc.garcol.exchange.proto.ClusterPayloadProto;
import gc.garcol.exchange.proto.ClusterServiceGrpc;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author thaivc
 * @since 2024
 */
@Slf4j
@Service
public class ClusterExchangeService
{
    @Value("${exchange.grpc.hosts}")
    private String[] grpcExchangeHosts;

    @Value("${exchange.grpc.ports}")
    private int[] grpcExchangePorts;

    @Value("${api.timeout-ms}")
    private long timeoutMilliseconds = 5000;

    private final Map<UUID, CompletableFuture<Response>> responseFutures = new ConcurrentHashMap<>();
    private final AtomicBoolean isClusterConnected = new AtomicBoolean(false);

    private ClusterServiceGrpc.ClusterServiceStub clusterServiceStub;
    private StreamObserver<ClusterPayloadProto.Request> requestStream;
    private StreamObserver<ClusterPayloadProto.Response> responseStream = new StreamObserver<>()
    {

        public void onNext(final ClusterPayloadProto.Response value)
        {

        }

        public void onError(final Throwable t)
        {
            log.info("ClusterExchangeStub responseStream onError: {}", t.getMessage());
            isClusterConnected.set(false);
        }

        public void onCompleted()
        {
            log.info("ClusterExchangeStub responseStream onCompleted");
            isClusterConnected.set(false);
        }
    };

    private void connectCluster()
    {
        // todo: chose right leader
        clusterServiceStub = ClusterServiceGrpc
            .newStub(ManagedChannelBuilder.forAddress(grpcExchangeHosts[0], grpcExchangePorts[0])
                .usePlaintext()
                .build());

        requestStream = clusterServiceStub.send(responseStream);
        isClusterConnected.set(true);
    }

    public CompletableFuture<Response> request(Request request)
    {
        UUID correlationId = UUID.randomUUID();
        CompletableFuture<Response> responseFuture = new CompletableFuture<>();
        responseFuture.completeOnTimeout(
            new ResponseTimeout(408, 408),
            timeoutMilliseconds,
            TimeUnit.MILLISECONDS
        );

        if (!isClusterConnected.get())
        {
            connectCluster();
        }
        responseFutures.put(correlationId, responseFuture);
        requestStream.onNext(RequestMapper.toProto(request));
        return responseFuture;
    }
}
