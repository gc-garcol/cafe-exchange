package gc.garcol.exchangecluster.configuration;

import gc.garcol.exchangecore.RedisService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.Jedis;

/**
 * @author thaivc
 * @since 2024
 */
@Configuration
public class ClusterConfiguration
{

    @Bean
    Jedis jedis(
        @Value("${redis.host}") String host,
        @Value("${redis.port}") int port
    )
    {
        return new Jedis(host, port);
    }

    @Bean
    public RedisService redisService(
        final Jedis jedis
    )
    {
        return new RedisService(jedis);
    }
}
