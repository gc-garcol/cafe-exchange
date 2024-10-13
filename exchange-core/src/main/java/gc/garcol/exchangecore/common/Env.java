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
    public static final int RECORDS_PER_TERM = Integer.parseInt(dotenv.get("RECORDS_PER_TERM"));

    public static final String LOG_DIR = dotenv.get("LOG_DIR");
    public static final String METADATA_FILE = dotenv.get("METADATA_FILE");
}
