package gc.garcol.exchangeclient.domain;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author thaivc
 * @since 2024
 */
public record QueryResponse(
    int status,
    JsonNode data
) implements Response
{
}
