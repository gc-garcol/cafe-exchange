package gc.garcol.exchangecore.exchangelog;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author thaivc
 * @since 2024
 */
public class LogUtil
{
    private static final int LONG_LENGTH = String.valueOf(Long.MAX_VALUE).length();
    private static final String LOG_FORMAT = "%0" + LONG_LENGTH + "d" + ".dat";
    private static final String INDEX_FORMAT = "%0" + LONG_LENGTH + "d" + ".index.dat";

    public static String logName(long term)
    {
        return String.format(LOG_FORMAT, term);
    }

    public static String indexName(long term)
    {
        return String.format(INDEX_FORMAT, term);
    }

    public static void createDirectoryNX(String pathStr)
    {
        Path path = Path.of(pathStr);
        if (!Files.exists(path))
        {
            try
            {
                Files.createDirectories(path);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }

}
