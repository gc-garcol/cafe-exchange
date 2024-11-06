package gc.garcol.exchangecore;

import gc.garcol.exchangecore.common.Env;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.Agent;

import java.util.UUID;

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
            nextHeartBeatTime = System.currentTimeMillis() + Env.HEARTBEAT_INTERVAL_MS;
            String currentLeader = ExchangeIOC.SINGLETON.getInstance(RedisService.class).acquireLeaderRole();
            ExchangeIOC.SINGLETON.getInstance(ExchangeCluster.class).enqueueHeartBeat(UUID.fromString(currentLeader));
        }
        return 0;
    }

    public String roleName()
    {
        return "HeartBeat";
    }
}
