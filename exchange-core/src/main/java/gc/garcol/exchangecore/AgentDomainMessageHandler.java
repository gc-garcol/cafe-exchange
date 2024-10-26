package gc.garcol.exchangecore;

import com.google.protobuf.InvalidProtocolBufferException;
import gc.garcol.exchange.proto.ClusterPayloadProto;
import gc.garcol.exchange.proto.CommandProto;
import gc.garcol.exchange.proto.QueryProto;
import gc.garcol.exchangecore.common.Env;
import gc.garcol.exchangecore.ringbuffer.ConsumerTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.Agent;

import java.nio.ByteBuffer;
import java.util.UUID;

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
    private final ByteBuffer cachedBuffer = ByteBuffer.allocateDirect(Env.MAX_COMMAND_SIZE);
    private final ExchangeCluster exchangeCluster;
    private StateMachineDelegate stateMachine;

    public int doWork() throws Exception
    {
        exchangeCluster.state.handleHeartBeatEvent();
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
            cachedBuffer.clear();
            buffer.getBytes(index + Long.BYTES * 2, cachedBuffer, length - Long.BYTES * 2);
            ClusterPayloadProto.Request request = ClusterPayloadProto.Request.parseFrom(cachedBuffer);

            UUID sender = new UUID(buffer.getLong(index), buffer.getLong(index + Long.BYTES));
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
        stateMachine.apply(command);
        // todo response
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
