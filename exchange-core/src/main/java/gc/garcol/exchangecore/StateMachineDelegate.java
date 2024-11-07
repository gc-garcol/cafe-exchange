package gc.garcol.exchangecore;

import gc.garcol.exchange.proto.CommandProto;
import gc.garcol.exchange.proto.QueryProto;
import gc.garcol.exchangecore.common.MessageCode;
import gc.garcol.exchangecore.common.StatusCode;
import gc.garcol.exchangecore.domain.ClusterResponse;
import gc.garcol.exchangecore.domain.CommonResponse;
import lombok.RequiredArgsConstructor;

/**
 * @author thaivc
 * @since 2024
 */
@RequiredArgsConstructor
public class StateMachineDelegate implements StateMachine
{

    private final StateMachineBalance stateMachineBalance;
    private final StateMachineOrder stateMachineOrder;
    private final StateMachineAsset stateMachineAsset;

    public CommonResponse apply(final CommandProto.Command command)
    {
        return switch (command.getPayloadCase())
        {
            case CREATEBALANCE, WITHDRAWN, DEPOSIT -> stateMachineBalance.apply(command);
            case NEWORDER, CANCELOPTIONORDER -> stateMachineOrder.apply(command);
            default -> new CommonResponse(StatusCode.BAD_REQUEST.code, MessageCode.COMMAND_TYPE_NOT_FOUND.code);
        };
    }

    public ClusterResponse query(final QueryProto.Query query)
    {
        return switch (query.getPayloadCase())
        {
            case BALANCEQUERY ->
            {
                var balance = stateMachineBalance.balance(query.getBalanceQuery().getOwnerId());
                yield balance != null
                    ? balance
                    : new CommonResponse(StatusCode.BAD_REQUEST.code, MessageCode.BALANCE_NOT_FOUND.code);
            }
            default -> new CommonResponse(StatusCode.BAD_REQUEST.code, MessageCode.QUERY_TYPE_NOT_FOUND.code);
        };
    }
}
