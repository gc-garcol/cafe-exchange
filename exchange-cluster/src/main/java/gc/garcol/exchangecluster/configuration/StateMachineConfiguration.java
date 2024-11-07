package gc.garcol.exchangecluster.configuration;

import gc.garcol.exchangecore.*;
import gc.garcol.exchangecore.exchangelog.PLogRepository;
import gc.garcol.exchangecore.exchangelog.PSnapshotRepository;
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
        PLogRepository logRepository,
        PSnapshotRepository snapshotRepository,
        StateMachineDelegate stateMachineDelegate
    )
    {
        return new StateMachineLoader(logRepository, snapshotRepository, stateMachineDelegate);
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
