package gc.garcol.exchangecore.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author thaivc
 * @since 2024
 */
@Getter
@Setter
@Accessors(chain = true, fluent = true)
@NoArgsConstructor
@AllArgsConstructor
public class Balance
{
    private long ownerId;
    private Map<String, BalanceAsset> assets;
    private Map<String, UUID> versions;

    public static Balance create(long ownerId)
    {
        return new Balance()
            .ownerId(ownerId)
            .assets(new HashMap<>())
            .versions(new HashMap<>());
    }
}
