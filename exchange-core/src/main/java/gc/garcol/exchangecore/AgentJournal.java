package gc.garcol.exchangecore;

import gc.garcol.exchange.proto.ClusterPayloadProto;
import gc.garcol.exchangecore.common.ClusterConstant;
import gc.garcol.exchangecore.common.Env;
import gc.garcol.libcore.OneToManyRingBuffer;
import gc.garcol.libcore.UnsafeBuffer;
import gc.garcol.walcore.LogRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
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
public class AgentJournal implements Agent
{

    final LogRepository logRepository;
    final OneToManyRingBuffer oneToManyRingBuffer;
    final ByteBuffer commandsBuilder = ByteBuffer.allocate(Env.BATCH_INSERT_SIZE * Env.MAX_COMMAND_SIZE);
    int messageCount;
    int nextIndex;

    public int doWork() throws Exception
    {
        messageCount = 0;
        nextIndex = Integer.BYTES;
        commandsBuilder.clear();
        this.oneToManyRingBuffer.read(0, this::consume, Env.BATCH_INSERT_SIZE);
        if (messageCount > 0)
        {
            commandsBuilder.putInt(0, messageCount);
            var messages = commandsBuilder.slice(0, nextIndex);

            logRepository.append(messages);
        }
        return 0;
    }

    public String roleName()
    {
        return "Journaler";
    }

    public boolean consume(final int msgTypeId, final UnsafeBuffer buffer, final int index, final int length)
    {
        try
        {
            if (msgTypeId == ClusterConstant.COMMAND_MSG_TYPE)
            {
                messageCount++;
                long requestId = buffer.getLong(index + Long.BYTES * 2);
                ClusterPayloadProto.Request request = ExchangePayloadHolder.REQUESTS.get(requestId);
                byte[] requestBytes = request.toByteArray();
                commandsBuilder.putInt(nextIndex, requestBytes.length);
                commandsBuilder.put(nextIndex + Integer.BYTES, requestBytes);
                nextIndex = nextIndex + Integer.BYTES + requestBytes.length;
            }
        }
        catch (Exception e)
        {
            log.error("Failed to parse request from log", e);
        }
        return true;
    }
}
