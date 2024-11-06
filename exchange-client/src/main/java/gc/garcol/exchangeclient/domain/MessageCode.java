package gc.garcol.exchangeclient.domain;

import lombok.RequiredArgsConstructor;

/**
 * @author thaivc
 * @since 2024
 */
@RequiredArgsConstructor
public enum MessageCode
{
    /**
     * Due to cannot publish message into RingBuffer
     */
    CLUSTER_BUSY(100),

    FOLLOWER_CANNOT_HANDLE_REQUEST(200),

    REQUEST_TYPE_NOT_FOUND(1_000),
    COMMAND_TYPE_NOT_FOUND(1_001),
    QUERY_TYPE_NOT_FOUND(1_002),

    MODIFIED_INSUFFICIENT_VERSION(2_000),

    BALANCE_NOT_FOUND(10_000),
    ASSET_NOT_FOUND(10_001),
    BALANCE_ASSET_NOT_FOUND(10_002),

    BALANCE_CREATED_SUCCESS(20_001),
    BALANCE_CREATED_FAILED(20_002),
    BALANCE_DEPOSIT_SUCCESS(20_003),
    BALANCE_DEPOSIT_FAILED(20_004),
    BALANCE_WITHDRAW_SUCCESS(20_005),
    BALANCE_WITHDRAW_FAILED_BALANCE_INSUFFICIENT(20_006),
    ;

    public final int code;

    public static MessageCode of(int code)
    {
        for (MessageCode messageCode : values())
        {
            if (messageCode.code == code)
            {
                return messageCode;
            }
        }
        return null;
    }
}
