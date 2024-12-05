package gc.garcol.benchmarkcluster.domain;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author thaivc
 * @since 2024
 */
public record RequestQueueItem(
    UUID correlationId,
    Request request,
    CompletableFuture<Response> responseFuture
)
{
}
