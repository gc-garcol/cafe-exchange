package gc.garcol.exchangecluster.exchangelog;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author thaivc
 * @since 2024
 */
public class FileUtil
{

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
