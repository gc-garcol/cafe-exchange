package gc.garcol.exchangecore.ringbuffer;

/**
 * Reference to the Agrona BitUtil class.
 */
public class BitUtil
{
    /**
     * <p>
     * Align a value to the next multiple up of alignment.
     * If the value equals an alignment multiple then it is returned unchanged.
     * <p>
     * This method executes without branching. This code is designed to be use in the fast path and should not
     * be used with negative numbers. Negative numbers will result in undefined behaviour.
     *
     * @param value     to be aligned up.
     * @param alignment to be used.
     * @return the value aligned to the next boundary.
     */
    public static int align(final int value, final int alignment)
    {
        return (value + (alignment - 1)) & -alignment;
    }
}
