package gc.garcol.exchangecore.domain;

import gc.garcol.exchange.proto.*;
import gc.garcol.exchangecore.common.StatusCode;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author thaivc
 * @since 2024
 */
public interface ClusterResponseMapper
{

    static CommonProto.OptimisticLock toVersionProto(Map<String, UUID> versions)
    {
        CommonProto.OptimisticLock.Builder builder = CommonProto.OptimisticLock.newBuilder();
        if (versions != null)
        {
            versions.forEach((key, lock) -> builder.putVersions(key, CommonProto.UUID.newBuilder()
                .setUuidMsb(lock.getMostSignificantBits())
                .setUuidLsb(lock.getLeastSignificantBits()).
                build())
            );
        }
        return builder.build();
    }

    static CommonProto.BigDecimal toBigDecimalProto(BigDecimal bigDecimal)
    {
        return CommonProto.BigDecimal.newBuilder()
            .setValue(bigDecimal.toString())
            .setScale(bigDecimal.scale())
            .build();
    }

    static Map<String, BalanceProto.BalanceAsset> toAssetProto(Map<String, BalanceAsset> assets)
    {
        Map<String, BalanceProto.BalanceAsset> result = new HashMap<>();
        assets.forEach((key, asset) -> result.put(key, BalanceProto.BalanceAsset.newBuilder()
            .setAsset(asset.name())
            .setVersion(toVersionProto(asset.versions()))
            .setAvailableAmount(toBigDecimalProto(asset.availableAmount()))
            .setLockAmount(toBigDecimalProto(asset.lockAmount()))
            .build()
        ));
        return result;
    }

    static ClusterPayloadProto.Response toProto(CommonProto.UUID correlationId, ClusterResponse response)
    {
        return switch (response)
        {
            case Balance balance -> parseProto(correlationId, balance);
            case Asset asset -> null; // todo
            default -> ClusterPayloadProto.Response.newBuilder()
                .setCorrelationId(correlationId)
                .setCommonResponse(
                    ClusterPayloadProto.CommonResponse.newBuilder()
                        .setCode(StatusCode.BAD_REQUEST.code)
                        .setStatus(404)
                        .build()
                )
                .build();
        };
    }

    static ClusterPayloadProto.Response parseProto(CommonProto.UUID correlationId, Balance balance)
    {
        BalanceProto.Balance balanceProto = BalanceProto.Balance.newBuilder()
            .setOwnerId(balance.ownerId())
            .putAllAssets(toAssetProto(balance.assets()))
            .setVersion(toVersionProto(balance.versions()))
            .build();
        QueryProto.QueryResponse queryResponse = QueryProto.QueryResponse.newBuilder()
            .setBalanceQueryResponse(
                BalanceQueryProto.BalanceQueryResponse.newBuilder()
                    .setBalance(balanceProto)
                    .build()
            )
            .build();
        ClusterPayloadProto.Response.Builder builder = ClusterPayloadProto.Response.newBuilder()
            .setCorrelationId(correlationId)
            .setQueryResponse(queryResponse);
        return builder.build();
    }
}
