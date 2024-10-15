package gc.garcol.exchangecore;

import gc.garcol.exchange.proto.CommandProto;
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

    public void apply(final CommandProto.Command command)
    {
        switch (command.getPayloadCase())
        {
            case CREATEBALANCE, WITHDRAWN, DEPOSIT -> stateMachineBalance.apply(command);
            case NEWORDER, CANCELOPTIONORDER -> stateMachineOrder.apply(command);
            default -> throw new IllegalStateException("Unexpected value: " + command.getParserForType());
        }
    }
}
