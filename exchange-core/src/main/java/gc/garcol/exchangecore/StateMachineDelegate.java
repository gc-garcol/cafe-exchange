package gc.garcol.exchangecore;

import gc.garcol.exchange.proto.CommandProto;
import gc.garcol.exchangecore.common.ResponseCode;
import gc.garcol.exchangecore.common.StatusCode;
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
            default -> new CommonResponse(StatusCode.BAD_REQUEST.code, ResponseCode.COMMAND_TYPE_NOT_FOUND.code);
        };
    }
}
