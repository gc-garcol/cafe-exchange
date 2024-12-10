package gc.garcol.exchangecluster.configuration.proxy;

import gc.garcol.exchangecore.common.Env;
import gc.garcol.walcore.LogRepository;
import org.springframework.stereotype.Repository;

/**
 * @author thaivc
 * @since 2024
 */
@Repository
public class LogRepositoryProxy extends LogRepository
{
    public LogRepositoryProxy()
    {
        super(Env.DATA_DIR + "/" + Env.LOG_DIR);
    }
}
