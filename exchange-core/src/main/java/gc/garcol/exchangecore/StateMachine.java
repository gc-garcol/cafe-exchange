package gc.garcol.exchangecore;

import gc.garcol.exchange.proto.CommandProto;
import gc.garcol.exchangecore.domain.CommonResponse;

/**
 * @author thaivc
 * @since 2024
 */
public interface StateMachine
{
    CommonResponse apply(CommandProto.Command command);
}
