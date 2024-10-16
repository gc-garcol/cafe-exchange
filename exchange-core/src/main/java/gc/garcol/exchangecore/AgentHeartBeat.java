package gc.garcol.exchangecore;

import gc.garcol.exchangecore.common.Env;
import gc.garcol.exchangecore.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.Agent;

/**
 * @author thaivc
 * @since 2024
 */
@Slf4j
@RequiredArgsConstructor
public class AgentHeartBeat implements Agent
{

    private final String message;

    private long nextHeartBeatTime = 0;

    public int doWork() throws Exception
    {
        if (nextHeartBeatTime < System.currentTimeMillis())
        {
            log.debug(message);
            nextHeartBeatTime = System.currentTimeMillis() + Env.HEARTBEAT_INTERVAL_MS;
            boolean success = ExchangeIOC.SINGLETON.getInstance(RedisService.class).acquireLeaderRole();
            ExchangeIOC.SINGLETON.getInstance(ExchangeCluster.class).enqueueHeartBeat(success);
        }
        return 0;
    }

    public String roleName()
    {
        return "HeartBeat";
    }
}
