package gc.garcol.exchangecore.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * @author thaivc
 * @since 2024
 */
@Getter
@Setter
@Accessors(chain = true, fluent = true)
public class Asset
{
    public static final int PRECISION = 8;

    private String id;
    private BigDecimal availableAmount;
    private BigDecimal lockAmount;
    private Map<String, UUID> versions;
}
