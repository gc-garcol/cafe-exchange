package gc.garcol.exchangeclient.domain;

/**
 * @author thaivc
 * @since 2024
 */
public record ResponseTimeout(
    int status,
    int code
) implements Response
{
}