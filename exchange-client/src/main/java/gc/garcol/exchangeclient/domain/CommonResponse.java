package gc.garcol.exchangeclient.domain;

/**
 * @author thaivc
 * @since 2024
 */
public record CommonResponse(
    int status,
    int code,
    String messageCode
) implements Response
{
}
