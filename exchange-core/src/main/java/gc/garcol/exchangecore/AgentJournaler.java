package gc.garcol.exchangecore;

import gc.garcol.exchangecore.common.Env;
import gc.garcol.exchangecore.ringbuffer.ConsumerTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.Agent;

/**
 * Journal command from {@link ExchangeCluster#commandsInboundRingBuffer}
 *
 * @author thaivc
 * @since 2024
 */
@Slf4j
@RequiredArgsConstructor
public class AgentJournaler extends ConsumerTemplate implements Agent
{
    public int doWork() throws Exception
    {
        this.poll(Env.BATCH_INSERT_SIZE);
        return 0;
    }

    public String roleName()
    {
        return "Journaler";
    }

    public boolean consume(final int msgTypeId, final MutableDirectBuffer buffer, final int index, final int length)
    {
        return true;
    }
}
