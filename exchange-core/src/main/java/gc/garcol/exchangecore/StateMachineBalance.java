package gc.garcol.exchangecore;

import gc.garcol.exchange.proto.BalanceCommandProto;
import gc.garcol.exchange.proto.CommandProto;
import gc.garcol.exchangecore.common.MessageCode;
import gc.garcol.exchangecore.common.StatusCode;
import gc.garcol.exchangecore.domain.Asset;
import gc.garcol.exchangecore.domain.Balance;
import gc.garcol.exchangecore.domain.BalanceAsset;
import gc.garcol.exchangecore.domain.CommonResponse;
import org.agrona.collections.Long2ObjectHashMap;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

/**
 * @author thaivc
 * @since 2024
 */
public class StateMachineBalance implements StateMachine
{

    Long2ObjectHashMap<Balance> balances = new Long2ObjectHashMap<>();

    public CommonResponse apply(final CommandProto.Command command)
    {
        return switch (command.getPayloadCase())
        {
            case CREATEBALANCE -> createBalance(command.getCreateBalance());
            case DEPOSIT -> deposit(command.getDeposit());
            case WITHDRAWN -> withdrawn(command.getWithdrawn());
            default -> new CommonResponse(StatusCode.BAD_REQUEST.code, MessageCode.BALANCE_NOT_FOUND.code);
        };
    }

    public Balance balance(final long ownerId)
    {
        return balances.get(ownerId);
    }

    private CommonResponse createBalance(final BalanceCommandProto.CreateBalance createBalance)
    {
        final long ownerId = createBalance.getOwnerId();
        if (!balances.containsKey(ownerId))
        {
            balances.put(ownerId, Balance.create(ownerId));
            return new CommonResponse(StatusCode.SUCCESS.code, MessageCode.BALANCE_CREATED_SUCCESS.code);
        }
        return new CommonResponse(StatusCode.BAD_REQUEST.code, MessageCode.BALANCE_CREATED_FAILED.code);
    }

    private CommonResponse deposit(final BalanceCommandProto.Deposit deposit)
    {
        final long ownerId = deposit.getOwnerId();
        Balance balance = balances.get(ownerId);
        if (Objects.isNull(balance))
        {
            return new CommonResponse(StatusCode.BAD_REQUEST.code, MessageCode.BALANCE_NOT_FOUND.code);
        }

        Asset asset = ExchangeIOC.SINGLETON.getInstance(StateMachineAsset.class).assets.get(deposit.getAsset());
        if (Objects.isNull(asset))
        {
            return new CommonResponse(StatusCode.BAD_REQUEST.code, MessageCode.ASSET_NOT_FOUND.code);
        }

        BalanceAsset balanceAsset = balance.assets().get(deposit.getAsset());
        if (Objects.isNull(balanceAsset))
        {
            balanceAsset = new BalanceAsset()
                .lockAmount(BigDecimal.ZERO)
                .availableAmount(BigDecimal.ZERO)
                .name(asset.name());
            balance.assets().put(deposit.getAsset(), balanceAsset);
        }

        if (deposit.hasVersion() && balanceAsset.versions().containsKey(deposit.getVersion().getLockName()))
        {
            UUID currentVersion = balanceAsset.versions().get(deposit.getVersion().getLockName());
            UUID requestCurrentVersion = deposit.getVersion().hasCurrentLock() ? new UUID(
                deposit.getVersion().getCurrentLock().getUuidMsb(),
                deposit.getVersion().getCurrentLock().getUuidLsb()
            ) : null;

            if (!Objects.equals(currentVersion, requestCurrentVersion))
            {
                return new CommonResponse(StatusCode.BAD_REQUEST.code, MessageCode.MODIFIED_INSUFFICIENT_VERSION.code);
            }
        }

        BigDecimal depositAmount = new BigDecimal(
            deposit.getAmount().getValue()
        ).setScale(asset.precision(), RoundingMode.HALF_UP);
        BigDecimal newAmount = balanceAsset.availableAmount().add(depositAmount);
        balanceAsset.availableAmount(newAmount);

        if (deposit.hasVersion())
        {
            UUID newVersion = new UUID(
                deposit.getVersion().getNewLock().getUuidMsb(),
                deposit.getVersion().getNewLock().getUuidLsb()
            );
            balanceAsset.versions().put(deposit.getVersion().getLockName(), newVersion);
        }
        return new CommonResponse(StatusCode.SUCCESS.code, MessageCode.BALANCE_DEPOSIT_SUCCESS.code);
    }

    private CommonResponse withdrawn(final BalanceCommandProto.Withdrawn withdrawn)
    {
        final long ownerId = withdrawn.getOwnerId();
        Balance balance = balances.get(ownerId);
        if (Objects.isNull(balance))
        {
            return new CommonResponse(StatusCode.BAD_REQUEST.code, MessageCode.BALANCE_NOT_FOUND.code);
        }

        Asset asset = ExchangeIOC.SINGLETON.getInstance(StateMachineAsset.class).assets.get(withdrawn.getAsset());
        if (Objects.isNull(asset))
        {
            return new CommonResponse(StatusCode.BAD_REQUEST.code, MessageCode.ASSET_NOT_FOUND.code);
        }

        BalanceAsset balanceAsset = balance.assets().get(withdrawn.getAsset());
        if (Objects.isNull(balanceAsset))
        {
            return new CommonResponse(StatusCode.BAD_REQUEST.code, MessageCode.BALANCE_ASSET_NOT_FOUND.code);
        }

        if (withdrawn.hasVersion() && balanceAsset.versions().containsKey(withdrawn.getVersion().getLockName()))
        {
            UUID currentVersion = balanceAsset.versions().get(withdrawn.getVersion().getLockName());
            UUID requestCurrentVersion = withdrawn.getVersion().hasCurrentLock() ? new UUID(
                withdrawn.getVersion().getCurrentLock().getUuidMsb(),
                withdrawn.getVersion().getCurrentLock().getUuidLsb()
            ) : null;

            if (!Objects.equals(currentVersion, requestCurrentVersion))
            {
                return new CommonResponse(StatusCode.BAD_REQUEST.code, MessageCode.MODIFIED_INSUFFICIENT_VERSION.code);
            }
        }

        BigDecimal withdrawnAmount = new BigDecimal(
            withdrawn.getAmount().getValue()
        ).setScale(asset.precision(), RoundingMode.HALF_UP);

        if (withdrawnAmount.compareTo(balanceAsset.availableAmount()) < 0)
        {
            return new CommonResponse(StatusCode.BAD_REQUEST.code, MessageCode.BALANCE_WITHDRAW_FAILED_BALANCE_INSUFFICIENT.code);
        }

        BigDecimal newAmount = balanceAsset.availableAmount().subtract(withdrawnAmount);
        balanceAsset.availableAmount(newAmount);

        if (withdrawn.hasVersion())
        {
            UUID newVersion = new UUID(
                withdrawn.getVersion().getNewLock().getUuidMsb(),
                withdrawn.getVersion().getNewLock().getUuidLsb()
            );
            balanceAsset.versions().put(withdrawn.getVersion().getLockName(), newVersion);
        }
        return new CommonResponse(StatusCode.SUCCESS.code, MessageCode.BALANCE_WITHDRAW_SUCCESS.code);
    }
}
