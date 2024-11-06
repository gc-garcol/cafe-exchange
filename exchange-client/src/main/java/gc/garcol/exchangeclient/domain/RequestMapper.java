package gc.garcol.exchangeclient.domain;

import gc.garcol.exchange.proto.*;
import gc.garcol.exchangeclient.transport.payload.BalanceCreateRequest;
import gc.garcol.exchangeclient.transport.payload.BalanceDepositRequest;
import gc.garcol.exchangeclient.transport.payload.BalanceQuery;

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
            case BalanceCreateRequest balanceCreateRequest -> parseProto(correlationId, balanceCreateRequest);
            case BalanceDepositRequest balanceDepositRequest -> parseProto(correlationId, balanceDepositRequest);
            case BalanceQuery balanceQuery -> parseProto(correlationId, balanceQuery);
            default -> throw new IllegalStateException("Unexpected value: " + request);
        };
    }

    private static ClusterPayloadProto.Request parseProto(UUID correlationId, BalanceCreateRequest balanceCreateRequest)
    {
        BalanceCommandProto.CreateBalance createBalance = BalanceCommandProto.CreateBalance.newBuilder()
            .setOwnerId(balanceCreateRequest.ownerId())
            .build();
        return ClusterPayloadProto.Request.newBuilder()
            .setCorrelationId(CommonProto.UUID.newBuilder()
                .setUuidMsb(correlationId.getMostSignificantBits())
                .setUuidLsb(correlationId.getLeastSignificantBits())
                .build()
            )
            .setCommand(CommandProto.Command.newBuilder()
                .setCreateBalance(createBalance)
                .build())
            .build();
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

    private static ClusterPayloadProto.Request parseProto(UUID correlationId, BalanceQuery balanceQuery)
    {
        BalanceQueryProto.BalanceQuery balanceQueryProto = BalanceQueryProto.BalanceQuery.newBuilder()
            .setOwnerId(balanceQuery.ownerId())
            .build();
        QueryProto.Query query = QueryProto.Query.newBuilder()
            .setBalanceQuery(balanceQueryProto)
            .build();
        return ClusterPayloadProto.Request.newBuilder()
            .setCorrelationId(CommonProto.UUID.newBuilder()
                .setUuidMsb(correlationId.getMostSignificantBits())
                .setUuidLsb(correlationId.getLeastSignificantBits())
                .build()
            )
            .setQuery(query)
            .build();
    }

}
