package gc.garcol.exchangecore.ringbuffer;

/**
 * @author thaivc
 * @since 2024
 */
@FunctionalInterface
public interface MessageHandler
{

    /**
     * Called for the processing of each message read from a buffer in turn.
     *
     * @param msgTypeId
     * @param buffer
     * @param index
     * @param length
     * @return should commit the consumed position or not
     */
    boolean onMessage(int msgTypeId, UnsafeBuffer buffer, int index, int length);
}
