package gc.garcol.exchangecore.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @author thaivc
 * @since 2024
 */
@Getter
@Setter
@Accessors(chain = true, fluent = true)
public class Asset
{
    private String name;
    private int precision;
}
