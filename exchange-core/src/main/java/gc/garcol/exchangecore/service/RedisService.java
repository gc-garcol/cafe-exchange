package gc.garcol.exchangecore.service;

import gc.garcol.exchangecore.common.ClusterGlobal;
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
    private final static String LEADER_ELECTION = """
        local currentLeader = redis.call('GET', KEYS[1])
        if currentLeader == ARGV[1] then
            -- Renew the TTL if the instance is the current leader
            redis.call('PEXPIRE', KEYS[1], ARGV[2])
            return currentLeader  -- Return that nodeID if EXPIRE is called
        else
            -- Try to set the leader if the key does not exist
            local setResult = redis.call('SET', KEYS[1], ARGV[1], 'NX', 'EX', ARGV[2])
            if setResult then
                return ARGV[1]  -- Return new-leader if SET is called
            else
                return currentLeader  -- Optionally return currentLeader if SET failed (key already exists)
            end
        end""";
    private final Jedis jedis;

    public String acquireLeaderRole()
    {
        var evalResult = jedis.eval(LEADER_ELECTION, 1, Env.LEADER_KEY, ClusterGlobal.NODE_ID_STR, Env.LEADER_TTL_MS + "");
        return (String)evalResult;
    }

    public String currentLeader()
    {
        return jedis.get(Env.LEADER_KEY);
    }

}
