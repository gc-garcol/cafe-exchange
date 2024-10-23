package gc.garcol.exchangecluster.configuration;

import gc.garcol.exchangecore.StateMachineAsset;
import gc.garcol.exchangecore.StateMachineBalance;
import gc.garcol.exchangecore.StateMachineDelegate;
import gc.garcol.exchangecore.StateMachineOrder;
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
    public StateMachineDelegate stateMachineDelegate(
        StateMachineBalance stateMachineBalance,
        StateMachineOrder stateMachineOrder
    )
    {
        return new StateMachineDelegate(stateMachineBalance, stateMachineOrder);
    }
}
