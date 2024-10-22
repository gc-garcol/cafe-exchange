package gc.garcol.exchangecore;

import gc.garcol.exchange.proto.ClusterPayloadProto;

import java.util.UUID;

/**
 * @author thaivc
 * @since 2024
 */
public interface ExchangeClusterState
{
    void start();

    void stop();

    boolean enqueueRequest(UUID sender, ClusterPayloadProto.Request request);

    void handleHeartBeatEvent();
}
