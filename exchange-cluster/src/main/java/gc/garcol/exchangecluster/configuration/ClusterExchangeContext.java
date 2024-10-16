package gc.garcol.exchangecluster.configuration;

import gc.garcol.exchangecore.ExchangeIOC;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * @author thaivc
 * @since 2024
 */
@Component
@RequiredArgsConstructor
public class ClusterExchangeContext extends ExchangeIOC
{
    private final ApplicationContext applicationContext;

    @PostConstruct
    void postConstruct()
    {
        ExchangeIOC.SINGLETON = this;
    }

    public <T> T getInstance(final Class<T> clazz)
    {
        return applicationContext.getBean(clazz);
    }
}
