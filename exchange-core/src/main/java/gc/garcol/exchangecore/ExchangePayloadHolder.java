package gc.garcol.exchangecore;

import gc.garcol.exchange.proto.ClusterPayloadProto;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author thaivc
 * @since 2024
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ExchangePayloadHolder
{
    public static final Map<Long, ClusterPayloadProto.Request> REQUESTS = new ConcurrentHashMap<>();
    public static final Map<Long, ClusterPayloadProto.Response> RESPONSES = new ConcurrentHashMap<>();

    public static final AtomicLong REQUEST_COUNTER = new AtomicLong(0);
    public static final AtomicLong RESPONSE_COUNTER = new AtomicLong(0);
}
