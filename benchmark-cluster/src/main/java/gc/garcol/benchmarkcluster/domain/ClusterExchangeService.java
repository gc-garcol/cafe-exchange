package gc.garcol.benchmarkcluster.domain;

import gc.garcol.exchange.proto.ClusterPayloadProto;
import gc.garcol.exchange.proto.ClusterServiceGrpc;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingIdleStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * @author thaivc
 * @since 2024
 */
@Slf4j
@Component
public class ClusterExchangeService
{
    @Value("${exchange.grpc.hosts}")
    private String[] grpcExchangeHosts;

    @Value("${exchange.grpc.ports}")
    private int[] grpcExchangePorts;

    private final Timer timer;

    private final int maxItemsPerConnection = 100_000;
    private final int idleFull = 10;

    public ClusterExchangeService(MeterRegistry meterRegistry)
    {
        this.timer = Timer.builder("cluster.exchange.grpc.request")
            .description("Cluster gRpc exchange request")
            .publishPercentileHistogram()
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);
    }

    private Map<UUID, Timer.Sample>[] startTimes;
    private AtomicInteger[] requestCount;
    private ClusterServiceGrpc.ClusterServiceStub clusterServiceStub;
    private StreamObserver<ClusterPayloadProto.Request>[] requestStreams;
    private StreamObserver<ClusterPayloadProto.Response>[] responseStreams;

    Function<Integer, StreamObserver<ClusterPayloadProto.Response>> createRequestStream = (index) -> new StreamObserver<>()
    {

        public void onNext(final ClusterPayloadProto.Response response)
        {
            UUID correlationId = new UUID(
                response.getCorrelationId().getUuidMsb(),
                response.getCorrelationId().getUuidLsb()
            );

            try
            {
                switch (response.getPayloadCase())
                {
                    case COMMONRESPONSE ->
                    {
                        Optional.ofNullable(startTimes[index].remove(correlationId))
                            .ifPresent(sampleTimer -> {
                                sampleTimer.stop(timer);
                            });
                    }
                }
            }
            catch (Exception e)
            {
                startTimes[index].remove(correlationId);
            }
            requestCount[index].decrementAndGet();
        }

        public void onError(final Throwable t)
        {
            log.info("ClusterExchangeStub responseStream onError: {}", t.getMessage());
        }

        public void onCompleted()
        {
            log.info("ClusterExchangeStub responseStream onCompleted");
        }
    };

    public Boolean request(int durationInSecond, int connection, Request request)
    {
        log.info("preparing...");
        if (clusterServiceStub == null)
        {
            clusterServiceStub = ClusterServiceGrpc
                .newStub(ManagedChannelBuilder.forAddress(grpcExchangeHosts[0], grpcExchangePorts[0])
                    .usePlaintext()
                    .build());
        }

        requestCount = new AtomicInteger[connection];
        requestStreams = new StreamObserver[connection];
        responseStreams = new StreamObserver[connection];
        startTimes = new ConcurrentHashMap[connection];

        for (int i = 0; i < connection; i++)
        {
            if (requestCount[i] == null)
            {
                requestCount[i] = new AtomicInteger(0);
            }
            connectCluster(i);
        }

        log.info("preparing done...");

        CompletableFuture[] futures = new CompletableFuture[connection];
        IdleStrategy idleStrategy = new SleepingIdleStrategy(idleFull);
        for (int connectionIndex = 0; connectionIndex < connection; connectionIndex++)
        {
            final int connectionIdx = connectionIndex;
            futures[connectionIndex] = CompletableFuture.runAsync(() -> {
                long start = System.currentTimeMillis();
                while (start + durationInSecond * 1_000L > System.currentTimeMillis())
                {
                    if (requestCount[connectionIdx].get() > maxItemsPerConnection)
                    {
                        idleStrategy.idle();
                        continue;
                    }
                    UUID correlationId = UUID.randomUUID();
                    startTimes[connectionIdx].put(correlationId, Timer.start());
                    send(correlationId, request, connectionIdx);
                    requestCount[connectionIdx].incrementAndGet();
                }
            });
        }
        CompletableFuture.allOf(futures).join();

        for (int i = 0; i < connection; i++)
        {
            requestStreams[i].onCompleted();
            responseStreams[i].onCompleted();
        }
        return true;
    }

    private void send(UUID correlationId, Request request, int connectionIndex)
    {
        requestStreams[connectionIndex].onNext(RequestMapper.toProto(correlationId, request));
    }

    private void connectCluster(int connectionIndex)
    {
        startTimes[connectionIndex] = new ConcurrentHashMap<>();
        responseStreams[connectionIndex] = createRequestStream.apply(connectionIndex);
        requestStreams[connectionIndex] = clusterServiceStub.send(responseStreams[connectionIndex]);
    }
}
