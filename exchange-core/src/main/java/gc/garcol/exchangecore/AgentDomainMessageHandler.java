package gc.garcol.exchangecore;

import com.google.protobuf.InvalidProtocolBufferException;
import gc.garcol.exchange.proto.ClusterPayloadProto;
import gc.garcol.exchange.proto.CommandProto;
import gc.garcol.exchange.proto.CommonProto;
import gc.garcol.exchange.proto.QueryProto;
import gc.garcol.exchangecore.common.Env;
import gc.garcol.exchangecore.domain.ClusterResponseMapper;
import gc.garcol.libcore.UnsafeBuffer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingMillisIdleStrategy;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handle all commands, queries and event effecting to {@link StateMachine} and {@link ExchangeCluster}
 *
 * @author thaivc
 * @since 2024
 */
@Slf4j
@RequiredArgsConstructor
public class AgentDomainMessageHandler implements Agent
{

    public static final AtomicBoolean IS_RUNNING = new AtomicBoolean(false);
    public static final AtomicInteger CONSUMER_INDEX = new AtomicInteger(0);

    private final ByteBuffer cachedBuffer = ByteBuffer.allocate(Env.MAX_COMMAND_SIZE);
    private final ExchangeCluster exchangeCluster;
    private StateMachineDelegate stateMachine;
    private final IdleStrategy responseIdle = new SleepingMillisIdleStrategy(1);

    public int doWork() throws Exception
    {
        if (!IS_RUNNING.get())
        {
            return 0;
        }
        exchangeCluster.state.handleHeartBeatEvent();

        if (exchangeCluster.state instanceof ExchangeClusterStateFollower)
        {
            return 0;
        }

        if (stateMachine == null)
        {
            stateMachine = ExchangeIOC.SINGLETON.getInstance(StateMachineDelegate.class);
        }
        this.exchangeCluster.requestRingBuffer.oneToManyRingBuffer()
            .read(CONSUMER_INDEX.get(), this::consume);
        return 0;
    }

    public boolean consume(final int msgTypeId, final UnsafeBuffer buffer, final int index, final int length)
    {
        try
        {
            UUID sender = new UUID(buffer.getLong(index), buffer.getLong(index + Long.BYTES));

            cachedBuffer.clear();
            buffer.getBytes(index + Long.BYTES * 2, cachedBuffer, 0, length - Long.BYTES * 2);
            cachedBuffer.position(length - Long.BYTES * 2);
            cachedBuffer.flip();
            ClusterPayloadProto.Request request = ClusterPayloadProto.Request.parseFrom(cachedBuffer);

            switch (request.getPayloadCase())
            {
                case COMMAND -> handleCommand(sender, request.getCorrelationId(), request.getCommand());
                case QUERY -> handleQuery(sender, request.getCorrelationId(), request.getQuery());
            }
        }
        catch (InvalidProtocolBufferException e)
        {
            log.error("Failed to parse command from log", e);
        }
        return true;
    }

    private void handleCommand(UUID sender, CommonProto.UUID correlationId, CommandProto.Command command)
    {
        var result = stateMachine.apply(command);
        var commonResponse = ClusterPayloadProto.CommonResponse.newBuilder()
            .setStatus(result.status())
            .setCode(result.code())
            .build();
        var grpcResponse = ClusterPayloadProto.Response.newBuilder()
            .setCorrelationId(correlationId)
            .setCommonResponse(commonResponse)
            .build();
        enqueueResponse(sender, grpcResponse);
    }

    private void enqueueResponse(UUID sender, ClusterPayloadProto.Response response)
    {
        for (; ; )
        {
            boolean success = exchangeCluster.enqueueResponse(sender, response);
            if (success)
            {
                break;
            }
            // backpressure
            responseIdle.idle();
        }
    }

    private void handleQuery(UUID sender, CommonProto.UUID correlationId, QueryProto.Query query)
    {
        var clusterResponse = stateMachine.query(query);
        var grpcResponse = ClusterResponseMapper.toProto(correlationId, clusterResponse);
        enqueueResponse(sender, grpcResponse);
    }

    public String roleName()
    {
        return "DomainLogic";
    }
}
