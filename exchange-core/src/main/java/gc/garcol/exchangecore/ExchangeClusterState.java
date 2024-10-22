package gc.garcol.exchangecore;

import gc.garcol.exchange.proto.CommandProto;

import java.util.UUID;

/**
 * @author thaivc
 * @since 2024
 */
public interface ExchangeClusterState
{
    void start();

    void stop();

    void handleHeartBeat();

    boolean handleCommands(UUID sender, CommandProto.Command command);
}
