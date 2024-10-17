package gc.garcol.exchangecore;

import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.ControlledMessageHandler;
import org.agrona.concurrent.SleepingMillisIdleStrategy;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author thaivc
 * @since 2024
 */
@Slf4j
public class ExchangeClusterStateFollower implements ExchangeClusterState
{
    private final ExchangeCluster exchangeCluster;

    private final AgentRunner replayLogRunner;
    private final AgentRunner heartBeatRunner;

    public ExchangeClusterStateFollower(final ExchangeCluster cluster)
    {
        this.exchangeCluster = cluster;
        var replayLogAgent = new AgentReplayLog();
        var heartBeatAgent = new AgentHeartBeat("Try to acquire leader role " + ClusterGlobal.NODE_ID);

        this.replayLogRunner = new AgentRunner(
            new SleepingMillisIdleStrategy(),
            error -> log.error("Follower replayLog error", error),
            null,
            replayLogAgent
        );
        this.heartBeatRunner = new AgentRunner(
            new SleepingMillisIdleStrategy(10),
            error -> log.error("Follower heartBeat error", error),
            null,
            heartBeatAgent
        );
    }

    public void start()
    {
        AgentRunner.startOnThread(heartBeatRunner);
        AgentRunner.startOnThread(replayLogRunner);
    }

    public void stop()
    {
        heartBeatRunner.close();
        replayLogRunner.close();
    }

    public void handleHeartBeat()
    {
        var ringBuffer = exchangeCluster.heartBeatInboundRingBuffer;

        AtomicBoolean becomeLeaderDetect = new AtomicBoolean(false);
        ringBuffer.controlledRead((messageType, buffer, offset, length) -> {
            if (becomeLeaderDetect.get())
            {
                return ControlledMessageHandler.Action.CONTINUE;
            }
            boolean tryBecomeLeaderSuccess = buffer.getByte(offset) == 1;
            if (tryBecomeLeaderSuccess)
            {
                becomeLeaderDetect.set(true);
            }
            return ControlledMessageHandler.Action.COMMIT;
        });
        if (becomeLeaderDetect.get())
        {
            exchangeCluster.transitionToLeader();
        }
    }
}
