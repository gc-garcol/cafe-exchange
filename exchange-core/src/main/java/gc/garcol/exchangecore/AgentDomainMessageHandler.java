package gc.garcol.exchangecore;

import com.google.protobuf.InvalidProtocolBufferException;
import gc.garcol.exchange.proto.ClusterPayloadProto;
import gc.garcol.exchange.proto.CommandProto;
import gc.garcol.exchange.proto.CommonProto;
import gc.garcol.exchange.proto.QueryProto;
import gc.garcol.exchangecore.common.Env;
import gc.garcol.exchangecore.ringbuffer.ConsumerTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingMillisIdleStrategy;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handle all commands, queries and event effecting to {@link StateMachine} and {@link ExchangeCluster}
 *
 * @author thaivc
 * @since 2024
 */
@Slf4j
@RequiredArgsConstructor
public class AgentDomainMessageHandler extends ConsumerTemplate implements Agent
{

    public static final AtomicBoolean IS_RUNNING = new AtomicBoolean(false);

    private final ByteBuffer cachedBuffer = ByteBuffer.allocateDirect(Env.MAX_COMMAND_SIZE);
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
        this.poll();
        return 0;
    }

    public boolean consume(final int msgTypeId, final MutableDirectBuffer buffer, final int index, final int length)
    {
        try
        {
            UUID sender = new UUID(buffer.getLong(index), buffer.getLong(index + Long.BYTES));

            cachedBuffer.clear();
            buffer.getBytes(index + Long.BYTES * 2, cachedBuffer, length - Long.BYTES * 2);
            cachedBuffer.flip();
            ClusterPayloadProto.Request request = ClusterPayloadProto.Request.parseFrom(cachedBuffer);

            switch (request.getPayloadCase())
            {
                case COMMAND -> handleCommand(sender, request.getCommand());
                case QUERY -> handleQuery(sender, request.getQuery());
            }
        }
        catch (InvalidProtocolBufferException e)
        {
            log.error("Failed to parse command from log", e);
        }
        return true;
    }

    private void handleCommand(UUID sender, CommandProto.Command command)
    {
        var correlationId = extractUUID(command);
        var result = stateMachine.apply(command);
        var commonResponse = ClusterPayloadProto.CommonResponse.newBuilder()
            .setCorrelationId(correlationId)
            .setStatus(result.status())
            .setCode(result.code())
            .build();
        var response = ClusterPayloadProto.Response.newBuilder()
            .setCommonResponse(commonResponse)
            .build();
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

    private CommonProto.UUID extractUUID(CommandProto.Command command)
    {
        return switch (command.getPayloadCase())
        {
            case CREATEBALANCE -> command.getCreateBalance().getCorrelationId();
            case DEPOSIT -> command.getDeposit().getCorrelationId();
            case WITHDRAWN -> command.getWithdrawn().getCorrelationId();
            case NEWORDER -> command.getNewOrder().getCorrelationId();
            case CANCELOPTIONORDER -> command.getCancelOptionOrder().getCorrelationId();
            case PAYLOAD_NOT_SET -> CommonProto.UUID.getDefaultInstance();
        };
    }

    private void handleQuery(UUID sender, QueryProto.Query query)
    {
        // todo get data from stateMachine
    }

    public String roleName()
    {
        return "DomainLogic";
    }
}
