package gc.garcol.exchangecore.ringbuffer;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author thaivc
 * @since 2024
 */
public class AtomicPointer
{

    AtomicReference<Pointer> pointer = new AtomicReference<>(new Pointer(1, 0, 0));

    public int index()
    {
        return pointer.get().index();
    }

    public long circle()
    {
        return pointer.get().circle();
    }

    public int length()
    {
        return pointer.get().length();
    }

    record Pointer(
        long circle,
        int index,
        int length
    )
    {
    }
}
