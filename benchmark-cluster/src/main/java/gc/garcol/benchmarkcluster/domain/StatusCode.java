package gc.garcol.benchmarkcluster.domain;

import lombok.RequiredArgsConstructor;

/**
 * @author thaivc
 * @since 2024
 */
@RequiredArgsConstructor
public enum StatusCode
{
    SUCCESS(200),
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    TIMEOUT(408),
    SERVER_BUSY(503),
    ;
    public final int code;

}
