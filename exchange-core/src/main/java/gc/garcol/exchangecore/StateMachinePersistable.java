package gc.garcol.exchangecore;

/**
 * @author thaivc
 * @since 2024
 */
public interface StateMachinePersistable
{
    void loadSnapshot();

    void snapshot();
}
