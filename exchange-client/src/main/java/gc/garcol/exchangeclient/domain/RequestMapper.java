package gc.garcol.exchangeclient.domain;

import gc.garcol.exchange.proto.ClusterPayloadProto;

/**
 * @author thaivc
 * @since 2024
 */
public interface RequestMapper
{
    static ClusterPayloadProto.Request toProto(Request request) {
        return null;
    }
}
