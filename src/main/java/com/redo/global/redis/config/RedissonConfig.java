package com.redo.global.redis.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    private static final String REDIS_PROTOCOL_PREFIX = "redis://";
    private static final int REDISSON_THREADS = 2;
    private static final int REDISSON_NETTY_THREADS = 2;
    private static final int MINIMUM_IDLE_CONNECTIONS = 1;
    private static final int CONNECTION_POOL_SIZE = 4;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(RedisConnectionDetails connectionDetails) {
        RedisConnectionDetails.Standalone standalone = connectionDetails.getStandalone();
        Config config = new Config();
        config.setThreads(REDISSON_THREADS);
        config.setNettyThreads(REDISSON_NETTY_THREADS);
        config.useSingleServer()
                .setAddress(REDIS_PROTOCOL_PREFIX + standalone.getHost() + ":" + standalone.getPort())
                .setDatabase(standalone.getDatabase())
                .setConnectionMinimumIdleSize(MINIMUM_IDLE_CONNECTIONS)
                .setConnectionPoolSize(CONNECTION_POOL_SIZE);

        return Redisson.create(config);
    }
}
