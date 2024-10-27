package gc.garcol.exchangecluster.configuration;

import gc.garcol.exchangecore.ExchangeCluster;
import gc.garcol.exchangecore.NetworkClusterService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author thaivc
 * @since 2024
 */
@Configuration
public class NetworkClusterServiceConfiguration
{
    @Bean
    NetworkClusterService networkClusterService(ExchangeCluster exchangeCluster)
    {
        return new NetworkClusterService(exchangeCluster);
    }
}
