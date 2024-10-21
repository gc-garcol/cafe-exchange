package gc.garcol.exchangecore.ringbuffer;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author thaivc
 * @since 2024
 */
public class AtomicPointer
{

    AtomicReference<Pointer> pointer = new AtomicReference<>(new Pointer(false, 0, 0));

    public int index()
    {
        return pointer.get().index();
    }

    public boolean flip()
    {
        return pointer.get().flip;
    }

    public int length()
    {
        return pointer.get().length();
    }

    record Pointer(
        boolean flip,
        int index,
        int length
    )
    {
    }
}
