package gc.garcol.exchangecore;

import gc.garcol.exchangecore.common.ClusterConstant;
import gc.garcol.exchangecore.common.Env;
import gc.garcol.exchangecore.ringbuffer.ConsumerTemplate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.Agent;

import java.nio.ByteBuffer;

/**
 * Journal request from {@link ExchangeCluster#requestAcceptorBuffer}
 *
 * @author thaivc
 * @since 2024
 */
@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
public class AgentJournal extends ConsumerTemplate implements Agent
{

    final ByteBuffer commandsBuilder = ByteBuffer.allocate(Env.BATCH_INSERT_SIZE * Env.MAX_COMMAND_SIZE);
    final ByteBuffer cachedBuffer = ByteBuffer.allocate(Env.MAX_COMMAND_SIZE);
    int messageCount;
    int nextIndex;

    public int doWork() throws Exception
    {
        messageCount = 0;
        nextIndex = Integer.BYTES;
        commandsBuilder.clear();
        this.poll(Env.BATCH_INSERT_SIZE);
        if (messageCount > 0)
        {
            commandsBuilder.putInt(0, messageCount);
            var messages = commandsBuilder.slice(0, nextIndex);
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
                messageCount++;
                int messageLength = length - Long.BYTES * 2;
                cachedBuffer.clear();
                buffer.getBytes(index + Long.BYTES * 2, cachedBuffer, messageLength);
                commandsBuilder.putInt(nextIndex, messageLength);
                commandsBuilder.put(nextIndex + Integer.BYTES, cachedBuffer, 0, messageLength);
                nextIndex = nextIndex + Integer.BYTES + messageLength;
            }
        }
        catch (Exception e)
        {
            log.error("Failed to parse request from log", e);
        }
        return true;
    }
}