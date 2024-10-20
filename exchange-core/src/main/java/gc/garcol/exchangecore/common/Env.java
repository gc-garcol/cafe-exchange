package gc.garcol.exchangecore.common;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * @author thaivc
 * @since 2024
 */
public class Env
{
    private static final Dotenv dotenv = Dotenv.load();

    public static final int BATCH_INSERT_SIZE = Integer.parseInt(dotenv.get("BATCH_INSERT_SIZE"));
    public static final int MAX_COMMAND_SIZE = Integer.parseInt(dotenv.get("MAX_COMMAND_SIZE"));
    public static final int RECORDS_PER_TERM = Integer.parseInt(dotenv.get("RECORDS_PER_TERM"));

    public static final String DATA_DIR = dotenv.get("DATA_DIR");
    public static final String LOG_DIR = dotenv.get("LOG_DIR");
    public static final String METADATA_FILE = dotenv.get("METADATA_FILE");
    public static final String SNAPSHOT_METADATA_FILE = dotenv.get("SNAPSHOT_METADATA_FILE");

    public static final long HEARTBEAT_INTERVAL_MS = Long.parseLong(dotenv.get("HEARTBEAT_INTERVAL_MS"));
    public static final String LEADER_KEY = dotenv.get("LEADER_KEY");
    public static final long LEADER_TTL_MS = Long.parseLong(dotenv.get("LEADER_TTL_MS"));

    public static final int BUFFER_SIZE_COMMAND_INBOUND_POW = Integer.parseInt(dotenv.get("BUFFER_SIZE_COMMAND_INBOUND_POW"));
    public static final int BUFFER_SIZE_COMMAND_INBOUND_ACCEPTOR_POW = Integer.parseInt(dotenv.get("BUFFER_SIZE_COMMAND_INBOUND_ACCEPTOR_POW"));
    public static final int BUFFER_SIZE_COMMAND_OUTBOUND_POW = Integer.parseInt(dotenv.get("BUFFER_SIZE_COMMAND_OUTBOUND_POW"));
    public static final int BUFFER_SIZE_HEARTBEAT_POW = Integer.parseInt(dotenv.get("BUFFER_SIZE_HEARTBEAT_POW"));
    public static final int BUFFER_SIZE_REPLAY_LOG_POW = Integer.parseInt(dotenv.get("BUFFER_SIZE_REPLAY_LOG_POW"));
}
