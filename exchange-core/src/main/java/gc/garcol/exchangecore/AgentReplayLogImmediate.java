package gc.garcol.exchangecore;

import com.google.protobuf.InvalidProtocolBufferException;
import gc.garcol.exchange.proto.CommandProto;
import gc.garcol.exchangecore.ringbuffer.ConsumerTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.Agent;

/**
 * Fetch data from log and push to relayLogInboundRingBuffer.
 *
 * @author thaivc
 * @since 2024
 */
@Slf4j
@RequiredArgsConstructor
public class AgentReplayLogImmediate extends ConsumerTemplate implements Agent
{

    private final StateMachine stateMachine;

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
        try
        {
            byte[] command = new byte[length];
            buffer.getBytes(index, command);
            stateMachine.apply(CommandProto.Command.parseFrom(command));
        }
        catch (InvalidProtocolBufferException e)
        {
            log.error("Failed to parse command from log", e);
        }
    }
}