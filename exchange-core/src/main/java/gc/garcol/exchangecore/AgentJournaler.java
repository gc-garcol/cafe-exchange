package gc.garcol.exchangecore;

import gc.garcol.exchange.proto.ClusterPayloadProto;
import gc.garcol.exchange.proto.CommandProto;
import gc.garcol.exchangecore.common.ClusterConstant;
import gc.garcol.exchangecore.common.Env;
import gc.garcol.exchangecore.ringbuffer.ConsumerTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.Agent;

/**
 * Journal request from {@link ExchangeCluster#requestAcceptorBuffer}
 *
 * @author thaivc
 * @since 2024
 */
@Slf4j
@RequiredArgsConstructor
public class AgentJournaler extends ConsumerTemplate implements Agent
{

    private CommandProto.Commands.Builder commandsBuilder;

    public int doWork() throws Exception
    {
        commandsBuilder = CommandProto.Commands.newBuilder();
        this.poll(Env.BATCH_INSERT_SIZE);
        if (commandsBuilder.getCommandsCount() > 0)
        {
            CommandProto.Commands commands = commandsBuilder.build();

            // todo journal
            log.info("Journaling commands: {}", commands);
        }
        return 0;
    }

    public String roleName()
    {
        return "Journaler";
    }

    public boolean consume(final int msgTypeId, final MutableDirectBuffer buffer, final int index, final int length)
    {
        try
        {
            if (msgTypeId == ClusterConstant.COMMAND_MSG_TYPE)
            {
                byte[] command = new byte[length];
                buffer.getBytes(index, command);
                ClusterPayloadProto.Request request = ClusterPayloadProto.Request.parseFrom(command);
                commandsBuilder.addCommands(request.getCommand());
            }
        }
        catch (Exception e)
        {
            log.error("Failed to parse request from log", e);
        }
        return true;
    }
}
