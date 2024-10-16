package gc.garcol.exchangecore.service;

import gc.garcol.exchangecore.ClusterGlobal;
import gc.garcol.exchangecore.common.Env;
import lombok.RequiredArgsConstructor;
import redis.clients.jedis.Jedis;

/**
 * @author thaivc
 * @since 2024
 */
@RequiredArgsConstructor
public class RedisService
{
    private final Jedis jedis;

    private final static String LEADER_ELECTION = """
        local currentLeader = redis.call('GET', KEYS[1])
        if currentLeader == ARGV[1] then
            -- Renew the TTL if the instance is the current leader
            redis.call('PEXPIRE', KEYS[1], ARGV[2])
            return 1  -- Return 1 if EXPIRE is called
        else
            -- Try to set the leader if the key does not exist
            local setResult = redis.call('SET', KEYS[1], ARGV[1], 'NX', 'EX', ARGV[2])
            if setResult then
                return 0  -- Return 0 if SET is called
            else
                return -1  -- Optionally return -1 if SET failed (key already exists)
            end
        end""";

    public boolean acquireLeaderRole()
    {
        var evalResult = jedis.eval(LEADER_ELECTION, 1, Env.LEADER_KEY, ClusterGlobal.NODE_ID, Env.LEADER_TTL_MS + "");
        return evalResult instanceof Long result && result > 0;
    }

    public String currentLeader()
    {
        return jedis.get(Env.LEADER_KEY);
    }

}
