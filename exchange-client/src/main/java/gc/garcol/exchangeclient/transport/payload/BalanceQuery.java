package gc.garcol.exchangeclient.transport.payload;

import gc.garcol.exchangeclient.domain.Request;

/**
 * @author thaivc
 * @since 2024
 */
public record BalanceQuery(
    long ownerId
) implements Request
{
}
