package gc.garcol.exchangecore;

import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.SleepingIdleStrategy;
import org.agrona.concurrent.SleepingMillisIdleStrategy;

/**
 * @author thaivc
 * @since 2024
 */
@Slf4j
public class ExchangeClusterStateLeader implements ExchangeClusterState
{
    private final ExchangeCluster cluster;

    private final AgentRunner heartBeatRunner;
    private final AgentRunner journalerRunner;

    public ExchangeClusterStateLeader(final ExchangeCluster cluster)
    {
        this.cluster = cluster;

        var heartBeatAgent = new AgentHeartBeat("Try to keep leader role");
        var journalerAgent = new AgentJournaler();

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
}
