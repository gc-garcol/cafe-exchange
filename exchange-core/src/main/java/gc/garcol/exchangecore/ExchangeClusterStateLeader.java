package gc.garcol.exchangecore;

import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.ControlledMessageHandler;
import org.agrona.concurrent.SleepingIdleStrategy;
import org.agrona.concurrent.SleepingMillisIdleStrategy;

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
        var journalerAgent = new AgentJournaler(exchangeCluster.commandsInboundRingBuffer);

        this.heartBeatRunner = new AgentRunner(
            new SleepingMillisIdleStrategy(10),
            error -> log.error("Leader heartBeat error", error),
            null,
            heartBeatAgent
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
