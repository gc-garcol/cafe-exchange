package gc.garcol.exchangecore;

import gc.garcol.exchange.proto.BalanceCommandProto;
import gc.garcol.exchange.proto.CommandProto;
import gc.garcol.exchangecore.common.ResponseCode;
import gc.garcol.exchangecore.common.StatusCode;
import gc.garcol.exchangecore.domain.Asset;
import gc.garcol.exchangecore.domain.Balance;
import gc.garcol.exchangecore.domain.CommonResponse;
import org.agrona.collections.Long2ObjectHashMap;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * @author thaivc
 * @since 2024
 */
public class StateMachineBalance implements StateMachine, StateMachinePersistable
{

    Long2ObjectHashMap<Balance> balances;

    public CommonResponse apply(final CommandProto.Command command)
    {
        return switch (command.getPayloadCase())
        {
            case CREATEBALANCE -> createBalance(command.getCreateBalance());
            case DEPOSIT -> deposit(command.getDeposit());
            case WITHDRAWN -> withdrawn(command.getWithdrawn());
            default -> new CommonResponse(StatusCode.BAD_REQUEST.code, ResponseCode.BALANCE_NOT_FOUND.code);
        };
    }

    private CommonResponse createBalance(final BalanceCommandProto.CreateBalance createBalance)
    {
        final long ownerId = createBalance.getOwnerId();
        if (!balances.containsKey(ownerId))
        {
            balances.put(ownerId, Balance.create(ownerId));
            return new CommonResponse(StatusCode.BAD_REQUEST.code, ResponseCode.BALANCE_CREATED_FAILED.code);
        }
        return new CommonResponse(StatusCode.SUCCESS.code, ResponseCode.BALANCE_CREATED_SUCCESS.code);
    }

    private CommonResponse deposit(final BalanceCommandProto.Deposit deposit)
    {
        final long ownerId = deposit.getOwnerId();
        Balance balance = balances.get(ownerId);
        if (Objects.isNull(balance))
        {
            return new CommonResponse(StatusCode.BAD_REQUEST.code, ResponseCode.BALANCE_NOT_FOUND.code);
        }
        Asset asset = balance.assets().get(deposit.getAsset());
        if (Objects.isNull(asset))
        {
            return new CommonResponse(StatusCode.BAD_REQUEST.code, ResponseCode.ASSET_NOT_FOUND.code);
        }
        BigDecimal depositAmount = new BigDecimal(deposit.getAmount().getValue()
            .toString()).setScale(Asset.PRECISION, RoundingMode.HALF_UP);
        BigDecimal newAmount = asset.availableAmount().add(depositAmount);
        asset.availableAmount(newAmount);
        return new CommonResponse(StatusCode.SUCCESS.code, ResponseCode.BALANCE_DEPOSIT_SUCCESS.code);
    }

    private CommonResponse withdrawn(final BalanceCommandProto.Withdrawn withdrawn)
    {
        final long ownerId = withdrawn.getOwnerId();
        Balance balance = balances.get(ownerId);
        if (Objects.isNull(balance))
        {
            return new CommonResponse(StatusCode.BAD_REQUEST.code, ResponseCode.BALANCE_NOT_FOUND.code);
        }
        Asset asset = balance.assets().get(withdrawn.getAsset());
        if (Objects.isNull(asset))
        {
            return new CommonResponse(StatusCode.BAD_REQUEST.code, ResponseCode.ASSET_NOT_FOUND.code);
        }
        BigDecimal withdrawnAmount = new BigDecimal(withdrawn.getAmount().getValue()
            .toString()).setScale(Asset.PRECISION, RoundingMode.HALF_UP);

        if (withdrawnAmount.compareTo(asset.availableAmount()) < 0)
        {
            return new CommonResponse(StatusCode.BAD_REQUEST.code, ResponseCode.BALANCE_WITHDRAW_FAILED_BALANCE_INSUFFICIENT.code);
        }

        BigDecimal newAmount = asset.availableAmount().subtract(withdrawnAmount);
        asset.availableAmount(newAmount);
        return new CommonResponse(StatusCode.SUCCESS.code, ResponseCode.BALANCE_WITHDRAW_SUCCESS.code);
    }

    public void loadSnapshot()
    {

    }

    public void snapshot()
    {

    }
}
