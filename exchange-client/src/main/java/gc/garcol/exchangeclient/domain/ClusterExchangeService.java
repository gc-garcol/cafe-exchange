package gc.garcol.exchangeclient.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.util.JsonFormat;
import gc.garcol.exchange.proto.ClusterPayloadProto;
import gc.garcol.exchange.proto.ClusterServiceGrpc;
import gc.garcol.exchange.proto.QueryProto;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.SleepingIdleStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author thaivc
 * @since 2024
 */
@Slf4j
@Service
public class ClusterExchangeService implements Agent
{
    @Value("${exchange.grpc.hosts}")
    private String[] grpcExchangeHosts;

    @Value("${exchange.grpc.ports}")
    private int[] grpcExchangePorts;

    @Value("${api.timeout-ms}")
    private long timeoutMilliseconds = 5000;

    private final Map<UUID, CompletableFuture<Response>> responseFutures = new ConcurrentHashMap<>();
    private final AtomicBoolean isClusterConnected = new AtomicBoolean(false);

    private AgentRunner requestConsumer;
    private final BlockingQueue<RequestQueueItem> requestQueue = new LinkedBlockingQueue<>();

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private ClusterServiceGrpc.ClusterServiceStub clusterServiceStub;
    private StreamObserver<ClusterPayloadProto.Request> requestStream;
    private StreamObserver<ClusterPayloadProto.Response> responseStream = new StreamObserver<>()
    {

        public void onNext(final ClusterPayloadProto.Response response)
        {
            UUID correlationId = new UUID(
                response.getCorrelationId().getUuidMsb(),
                response.getCorrelationId().getUuidLsb()
            );

            try
            {
                // todo refactor
                switch (response.getPayloadCase())
                {
                    case COMMONRESPONSE ->
                    {
                        ClusterPayloadProto.CommonResponse commonResponse = response.getCommonResponse();

                        Optional.ofNullable(responseFutures.remove(correlationId))
                            .ifPresent(responseFuture -> responseFuture.complete(new CommonResponse(
                                commonResponse.getStatus(),
                                commonResponse.getCode(),
                                Optional.ofNullable(MessageCode.of(commonResponse.getCode()))
                                    .map(MessageCode::toString)
                                    .orElse("Unknown message code"))
                            ));
                    }
                    case QUERYRESPONSE ->
                    {
                        QueryProto.QueryResponse queryResponse = response.getQueryResponse();
                        var responseDataJson = JsonFormat.printer().print(queryResponse);
                        var responseData = jsonMapper.readTree(responseDataJson);
                        Optional.ofNullable(responseFutures.remove(correlationId))
                            .ifPresent(responseFuture -> responseFuture.complete(new QueryResponse(
                                StatusCode.SUCCESS.code,
                                responseData.fields().next().getValue()
                            )));
                    }
                }
            }
            catch (Exception e)
            {
                Optional.ofNullable(responseFutures.remove(correlationId))
                    .ifPresent(responseFuture -> responseFuture.complete(new CommonResponse(500, 500, e.getMessage())));
            }
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

    @PostConstruct
    private void init()
    {
        requestConsumer = new AgentRunner(
            new SleepingIdleStrategy(),
            error -> log.error("ClusterExchangeService requestConsumer error", error),
            null,
            this
        );
        AgentRunner.startOnThread(requestConsumer);
    }

    @PreDestroy
    private void destroy()
    {
        requestConsumer.close();
    }

    public CompletableFuture<Response> request(Request request)
    {
        UUID correlationId = UUID.randomUUID();
        CompletableFuture<Response> responseFuture = new CompletableFuture<>();
        responseFuture.completeOnTimeout(
            new ResponseTimeout(StatusCode.TIMEOUT.code, 408),
            timeoutMilliseconds,
            TimeUnit.MILLISECONDS
        ).thenAccept((response) -> responseFutures.remove(correlationId));

        if (!isClusterConnected.get())
        {
            synchronized (this)
            {
                if (!isClusterConnected.get())
                {
                    responseFutures.clear();
                    connectCluster();
                }
            }
        }
        requestQueue.add(new RequestQueueItem(correlationId, request, responseFuture));
        return responseFuture;
    }

    private void send(UUID correlationId, Request request)
    {
        requestStream.onNext(RequestMapper.toProto(correlationId, request));
    }

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

    public int doWork() throws Exception
    {
        RequestQueueItem item;
        while ((item = requestQueue.poll()) != null)
        {
            responseFutures.put(item.correlationId(), item.responseFuture());
            send(item.correlationId(), item.request());
        }
        return 0;
    }

    public String roleName()
    {
        return "Cluster exchange service";
    }
}
