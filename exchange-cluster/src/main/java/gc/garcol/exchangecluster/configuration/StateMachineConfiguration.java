package gc.garcol.exchangecluster.configuration;

import gc.garcol.exchangecore.*;
import gc.garcol.walcore.LogRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author thaivc
 * @since 2024
 */
@Configuration
public class StateMachineConfiguration
{

    @Bean
    public StateMachineBalance stateMachineBalance()
    {
        return new StateMachineBalance();
    }

    @Bean
    public StateMachineOrder stateMachineOrder()
    {
        return new StateMachineOrder();
    }

    @Bean
    public StateMachineAsset stateMachineAsset()
    {
        return new StateMachineAsset();
    }

    @Bean
    public StateMachineLoader stateMachineLoader(
        LogRepository logRepository,
        StateMachineDelegate stateMachineDelegate
    )
    {
        return new StateMachineLoader(logRepository, stateMachineDelegate);
    }

    @Bean
    public StateMachineDelegate stateMachineDelegate(
        StateMachineBalance stateMachineBalance,
        StateMachineOrder stateMachineOrder,
        StateMachineAsset stateMachineAsset
    )
    {
        return new StateMachineDelegate(stateMachineBalance, stateMachineOrder, stateMachineAsset);
    }
}
