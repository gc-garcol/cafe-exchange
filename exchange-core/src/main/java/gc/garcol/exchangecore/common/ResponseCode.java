package gc.garcol.exchangecore.common;

import lombok.RequiredArgsConstructor;

/**
 * @author thaivc
 * @since 2024
 */
@RequiredArgsConstructor
public enum ResponseCode
{

    /**
     * Due to cannot publish message into RingBuffer
     */
    CLUSTER_BUSY(100),

    FOLLOWER_CANNOT_HANDLE_REQUEST(200),

    COMMAND_TYPE_NOT_FOUND(1_000),

    MODIFIED_INSUFFICIENT_VERSION(2_000),

    BALANCE_NOT_FOUND(10_000),
    ASSET_NOT_FOUND(10_001),
    BALANCE_ASSET_NOT_FOUND(10_002),

    BALANCE_CREATED_SUCCESS(10_002),
    BALANCE_CREATED_FAILED(10_003),
    BALANCE_DEPOSIT_SUCCESS(10_004),
    BALANCE_DEPOSIT_FAILED(10_005),
    BALANCE_WITHDRAW_SUCCESS(10_006),
    BALANCE_WITHDRAW_FAILED_BALANCE_INSUFFICIENT(10_007),
    ;

    public final int code;

}
