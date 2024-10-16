package gc.garcol.exchangecore;

import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.SleepingIdleStrategy;

/**
 * @author thaivc
 * @since 2024
 */
@Slf4j
public class BootstrapCluster implements Bootstrap
{

    private AgentRunner exchangeClusterRunner;

    public void start()
    {
        exchangeClusterRunner = new AgentRunner(
            new SleepingIdleStrategy(),
            error -> log.error("Exchange cluster duty cycle error", error),
            null,
            ExchangeIOC.SINGLETON.getInstance(ExchangeCluster.class));

        AgentRunner.startOnThread(exchangeClusterRunner);
    }

    public void stop()
    {
        exchangeClusterRunner.close();
    }
}
