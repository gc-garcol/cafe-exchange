package gc.garcol.exchangeclient.transport.payload;

import gc.garcol.exchangeclient.domain.Request;

/**
 * @author thaivc
 * @since 2024
 */
public record BalanceCreateRequest(
    long ownerId
) implements Request
{
}
