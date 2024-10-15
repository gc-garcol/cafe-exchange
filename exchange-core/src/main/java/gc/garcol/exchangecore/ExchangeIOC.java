package gc.garcol.exchangecore;

/**
 * @author thaivc
 * @since 2024
 */
public interface ExchangeIOC
{
    ExchangeIOC SINGLETON = new BlankIOC();

    <T> T getInstance(Class<T> clazz);

    class BlankIOC implements ExchangeIOC
    {
        @Override
        @SuppressWarnings("unchecked")
        public <T> T getInstance(Class<T> clazz)
        {
            return (T)new Object();
        }
    }
}
