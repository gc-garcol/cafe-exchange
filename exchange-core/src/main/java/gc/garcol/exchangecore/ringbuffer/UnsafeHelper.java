package gc.garcol.exchangecore.ringbuffer;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * @author thaivc
 * @since 2024
 */
public class UnsafeHelper
{
    static final Unsafe UNSAFE;

    static
    {
        try
        {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (Unsafe)theUnsafe.get(null);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Unable to load Unsafe", e);
        }
    }
}
