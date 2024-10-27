package gc.garcol.exchangeclient.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @author thaivc
 * @since 2024
 */
@Getter
@Setter
@NoArgsConstructor
@Accessors(fluent = true)
public class ResponseTimeout implements Response
{
    int status;
    int code;
}
