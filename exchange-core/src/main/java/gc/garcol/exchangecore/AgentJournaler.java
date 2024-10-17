package gc.garcol.exchangecore;

import gc.garcol.exchange.proto.CommandProto;
import gc.garcol.exchangecore.common.Env;
import gc.garcol.exchangecore.exchangelog.PLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.ringbuffer.OneToOneRingBuffer;

/**
 * Journal command from {@link ExchangeCluster#commandsInboundRingBuffer}
 *
 * @author thaivc
 * @since 2024
 */
@Slf4j
@RequiredArgsConstructor
public class AgentJournaler implements Agent
{
    final OneToOneRingBuffer commandsInboundRingBuffer;

    public int doWork() throws Exception
    {
        var logRepository = ExchangeIOC.SINGLETON.getInstance(PLogRepository.class);
        CommandProto.Commands.Builder commandBuilder = CommandProto.Commands.newBuilder();
        commandsInboundRingBuffer.read((msgTypeId, buffer, index, length) -> {
            try
            {
//                UUID uuid = new UUID(buffer.getLong(index), buffer.getLong(index + 8));
                byte[] commandBytes = new byte[length - 16];
                buffer.getBytes(index + 16, commandBytes);
                CommandProto.Command command = CommandProto.Command.parseFrom(commandBytes);

                // todo precheck with optimistic lock to avoid log write conflict (e.g. two leaders write)
                commandBuilder.addCommands(command);
            }
            catch (Exception e)
            {
                log.error("Failed to parse command from ring buffer", e);
            }
        }, Env.BATCH_INSERT_SIZE);

        if (commandBuilder.getCommandsCount() > 0)
        {
            logRepository.write(commandBuilder.build());
        }
        return 0;
    }

    public String roleName()
    {
        return "Journaler";
    }
}
