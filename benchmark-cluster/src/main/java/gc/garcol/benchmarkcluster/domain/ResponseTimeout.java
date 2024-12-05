package gc.garcol.benchmarkcluster.domain;

/**
 * @author thaivc
 * @since 2024
 */
public record ResponseTimeout(
    int status,
    int code,
    String message
) implements Response
{
    public ResponseTimeout(int status, int code)
    {
        this(status, code, "Timeout!!");
    }
}
