package gc.garcol.exchangecluster.configuration;

import gc.garcol.exchangecore.BootstrapCluster;
import gc.garcol.exchangecore.ExchangeCluster;
import gc.garcol.exchangecore.StateMachineLoader;
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
    BootstrapCluster bootstrapCluster(ExchangeCluster exchangeCluster, StateMachineLoader stateMachineLoader)
    {
        return new BootstrapCluster(exchangeCluster, stateMachineLoader);
    }
}
