package gc.garcol.exchangecore;

import gc.garcol.exchange.proto.CommandProto;
import gc.garcol.exchangecore.domain.CommonResponse;

/**
 * @author thaivc
 * @since 2024
 */
public class StateMachineOrder implements StateMachine
{
    public CommonResponse apply(final CommandProto.Command command)
    {
        return null;
    }
}
