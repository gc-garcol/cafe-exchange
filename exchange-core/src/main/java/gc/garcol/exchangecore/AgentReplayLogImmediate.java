package gc.garcol.exchangecore;

import gc.garcol.exchangecore.ringbuffer.ConsumerTemplate;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.Agent;

/**
 * Fetch data from log and push to relayLogInboundRingBuffer.
 *
 * @author thaivc
 * @since 2024
 */
public class AgentReplayLogImmediate extends ConsumerTemplate implements Agent
{
    public int doWork() throws Exception
    {
        this.poll();
        return 0;
    }

    public String roleName()
    {
        return "ReplayLog";
    }

    public void consume(final int msgTypeId, final MutableDirectBuffer buffer, final int index, final int length)
    {

    }
}