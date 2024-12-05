package gc.garcol.benchmarkcluster.transport.payload;

import gc.garcol.benchmarkcluster.domain.Request;

/**
 * @author thaivc
 * @since 2024
 */
public record BalanceDepositRequest(
    long ownerId,
    String asset,
    String amount
) implements Request
{
}
