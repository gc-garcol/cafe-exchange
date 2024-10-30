package gc.garcol.exchangeclient.domain;

import gc.garcol.exchange.proto.BalanceCommandProto;
import gc.garcol.exchange.proto.ClusterPayloadProto;
import gc.garcol.exchange.proto.CommandProto;
import gc.garcol.exchange.proto.CommonProto;
import gc.garcol.exchangeclient.transport.payload.BalanceCreateRequest;

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
            case BalanceCreateRequest balanceCreateRequest -> toProto(correlationId, balanceCreateRequest);
            default -> throw new IllegalStateException("Unexpected value: " + request);
        };
    }

    private static ClusterPayloadProto.Request toProto(UUID correlationId, BalanceCreateRequest balanceCreateRequest)
    {
        BalanceCommandProto.CreateBalance createBalance = BalanceCommandProto.CreateBalance.newBuilder()
            .setCorrelationId(
                CommonProto.UUID.newBuilder()
                    .setUuidMsb(correlationId.getMostSignificantBits())
                    .setUuidLsb(correlationId.getLeastSignificantBits())
                    .build()
            )
            .setOwnerId(balanceCreateRequest.ownerId())
            .build();
        return ClusterPayloadProto.Request.newBuilder()
            .setCommand(CommandProto.Command.newBuilder()
                .setCreateBalance(createBalance)
                .build())
            .build();
    }
}
