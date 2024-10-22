package gc.garcol.exchangecore.common;

import lombok.RequiredArgsConstructor;

/**
 * @author thaivc
 * @since 2024
 */
@RequiredArgsConstructor
public enum ErrorCode
{
    FOLLOWER_CANNOT_HANDLE_COMMAND(1);

    public final int code;

}
