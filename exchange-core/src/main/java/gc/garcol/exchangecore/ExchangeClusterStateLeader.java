package gc.garcol.exchangecore;

import gc.garcol.exchangecore.common.Env;
import gc.garcol.exchangecore.ringbuffer.RingBufferOneToMany;
import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.ControlledMessageHandler;
import org.agrona.concurrent.SleepingIdleStrategy;
import org.agrona.concurrent.SleepingMillisIdleStrategy;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author thaivc
 * @since 2024
 */
@Slf4j
public class ExchangeClusterStateLeader implements ExchangeClusterState
{
    private final ExchangeCluster exchangeCluster;

    private final AgentRunner heartBeatRunner;
    private final AgentRunner journalerRunner;

    public ExchangeClusterStateLeader(final ExchangeCluster cluster)
    {
        this.exchangeCluster = cluster;

        var heartBeatAgent = new AgentHeartBeat("Try to keep leader role " + ClusterGlobal.NODE_ID);

        this.heartBeatRunner = new AgentRunner(
            new SleepingMillisIdleStrategy(10),
            error -> log.error("Leader heartBeat error", error),
            null,
            heartBeatAgent
        );

        var journalerAgent = new AgentJournaler();
        var replayLogAgent = new AgentReplayLogImmediate();

        // todo clean old buffer
        this.exchangeCluster.commandsCommandRingBuffer = new RingBufferOneToMany(
            Env.BUFFER_SIZE_COMMAND_INBOUND_POW,
            List.of(journalerAgent, replayLogAgent)
        );

        this.journalerRunner = new AgentRunner(
            new SleepingIdleStrategy(1),
            error -> log.error("Leader journaler error", error),
            null,
            journalerAgent
        );
    }

    public void start()
    {
        ClusterGlobal.ENABLE_COMMAND_INBOUND.set(true);
        AgentRunner.startOnThread(heartBeatRunner);
        AgentRunner.startOnThread(journalerRunner);
    }

    public void stop()
    {
        ClusterGlobal.ENABLE_COMMAND_INBOUND.set(false);
        heartBeatRunner.close();
        journalerRunner.close();
    }

    public void handleHeartBeat()
    {
        var ringBuffer = exchangeCluster.heartBeatInboundRingBuffer;

        AtomicBoolean keepLeaderState = new AtomicBoolean(true);
        ringBuffer.controlledRead((messageType, buffer, offset, length) -> {
            if (!keepLeaderState.get())
            {
                return ControlledMessageHandler.Action.CONTINUE;
            }
            boolean keepLeaderStateSuccess = buffer.getByte(offset) == 1;
            if (!keepLeaderStateSuccess)
            {
                keepLeaderState.set(false);
            }
            return ControlledMessageHandler.Action.COMMIT;
        });
        if (!keepLeaderState.get())
        {
            exchangeCluster.transitionToFollower();
        }
    }
}
