package gc.garcol.exchangecore.common;

import lombok.NoArgsConstructor;

/**
 * @author thaivc
 * @since 2024
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class ClusterConstant
{
    public static final int COMMAND_MSG_TYPE = 10;
    public static final int QUERY_MSG_TYPE = 11;

    public static final int LONG_LENGTH = 8;
    public static final int UUID_LENGTH = 16;

    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
}
