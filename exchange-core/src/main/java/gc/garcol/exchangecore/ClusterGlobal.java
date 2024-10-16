package gc.garcol.exchangecore;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author thaivc
 * @since 2024
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ClusterGlobal
{
    public static final String NODE_ID = UUID.randomUUID().toString();

    public static final AtomicBoolean ENABLE_COMMAND_INBOUND = new AtomicBoolean(false);
}
