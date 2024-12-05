package gc.garcol.benchmarkcluster.domain;

import gc.garcol.benchmarkcluster.transport.payload.BalanceDepositRequest;
import gc.garcol.exchange.proto.BalanceCommandProto;
import gc.garcol.exchange.proto.ClusterPayloadProto;
import gc.garcol.exchange.proto.CommandProto;
import gc.garcol.exchange.proto.CommonProto;

import java.util.UUID;

/**
 * @author thaivc
 * @since 2024
 */
public interface RequestMapper
{
    static ClusterPayloadProto.Request toProto(UUID correlationId, Request request)
    {
        return switch (request)
        {
            case BalanceDepositRequest balanceDepositRequest -> parseProto(correlationId, balanceDepositRequest);
            default -> throw new IllegalStateException("Unexpected value: " + request);
        };
    }

    private static ClusterPayloadProto.Request parseProto(UUID correlationId, BalanceDepositRequest balanceDepositRequest)
    {
        CommonProto.BigDecimal amount = CommonProto.BigDecimal.newBuilder()
            .setScale(0)
            .setValue(balanceDepositRequest.amount())
            .build();
        BalanceCommandProto.Deposit deposit = BalanceCommandProto.Deposit.newBuilder()
            .setAmount(amount)
            .setOwnerId(balanceDepositRequest.ownerId())
            .setAsset(balanceDepositRequest.asset())
            .build();
        return ClusterPayloadProto.Request.newBuilder()
            .setCorrelationId(CommonProto.UUID.newBuilder()
                .setUuidMsb(correlationId.getMostSignificantBits())
                .setUuidLsb(correlationId.getLeastSignificantBits())
                .build()
            )
            .setCommand(CommandProto.Command.newBuilder()
                .setDeposit(deposit)
                .build())
            .build();
    }
}
