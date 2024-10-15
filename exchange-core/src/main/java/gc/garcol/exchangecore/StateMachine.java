package gc.garcol.exchangecore;

import gc.garcol.exchange.proto.CommandProto;

/**
 * @author thaivc
 * @since 2024
 */
public interface StateMachine
{
    void apply(CommandProto.Command command);
}
