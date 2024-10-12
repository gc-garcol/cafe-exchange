package gc.garcol.exchangecore;

import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.SleepingMillisIdleStrategy;

/**
 * @author thaivc
 * @since 2024
 */
@Slf4j
public class ExchangeClusterStateFollower implements ExchangeClusterState
{
    private final ExchangeCluster cluster;

    private final AgentRunner replayLogRunner;
    private final AgentRunner heartBeatRunner;

    public ExchangeClusterStateFollower(final ExchangeCluster cluster)
    {
        this.cluster = cluster;
        var replayLogAgent = new AgentReplayLog();
        var heartBeatAgent = new AgentHeartBeat();

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
}
