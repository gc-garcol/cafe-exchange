package gc.garcol.exchangecluster.configuration;

import gc.garcol.exchangecore.BootstrapCluster;
import gc.garcol.exchangecore.ExchangeCluster;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author thaivc
 * @since 2024
 */
@Configuration
public class BootstrapConfiguration
{
    @Bean
    BootstrapCluster bootstrapCluster(ExchangeCluster exchangeCluster)
    {
        return new BootstrapCluster(exchangeCluster);
    }
}
