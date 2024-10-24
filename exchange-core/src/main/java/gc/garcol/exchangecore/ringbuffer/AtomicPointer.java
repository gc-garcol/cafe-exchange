package gc.garcol.exchangecore.ringbuffer;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author thaivc
 * @since 2024
 */
@Getter
@Accessors(fluent = true)
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

    public record Pointer(
        boolean flip,
        int index,
        int length
    )
    {
    }
}
