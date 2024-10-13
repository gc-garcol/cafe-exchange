package gc.garcol.exchangecore.exchangelog;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @author thaivc
 * @since 2024
 */
@Getter
@Setter
@Accessors(fluent = true, chain = true)
public class ELogMetadata
{
    private long currentSegment = 1;
}
