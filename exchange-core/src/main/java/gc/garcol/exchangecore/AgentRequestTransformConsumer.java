package gc.garcol.exchangecore;

import gc.garcol.exchangecore.ringbuffer.ManyToManyRingBuffer;
import lombok.RequiredArgsConstructor;
import org.agrona.concurrent.Agent;

/**
 * @author thaivc
 * @since 2024
 */
@RequiredArgsConstructor
public class AgentRequestTransformConsumer implements Agent
{

    private final ManyToManyRingBuffer ringBuffer;

    public int doWork() throws Exception
    {
        ringBuffer.transfer();
        return 0;
    }

    public String roleName()
    {
        return "CommandInboundTransformer";
    }
}
