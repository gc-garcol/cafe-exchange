package gc.garcol.exchangecore;

/**
 * @author thaivc
 * @since 2024
 */
public abstract class ExchangeIOC
{
    public static ExchangeIOC SINGLETON;

    protected abstract <T> T getInstance(Class<T> clazz);
}
