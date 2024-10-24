package gc.garcol.exchangecore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author thaivc
 * @since 2024
 */
@Slf4j
@RequiredArgsConstructor
public class BootstrapCluster implements Bootstrap
{

    private final ExchangeCluster exchangeCluster;

    public void start()
    {
        exchangeCluster.onStart();
    }

    public void stop()
    {
        exchangeCluster.stopAll();
    }
}
