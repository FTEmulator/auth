package com.ftemulator.auth.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private int redisPort;

    @Value("${spring.redis.password}")
    private String redisPassword;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        config.setPassword(RedisPassword.of(redisPassword));
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        return template;
    }

    @Bean
    @Profile("!test")
    public ApplicationRunner redisInitializer(RedisConnectionFactory connectionFactory) {
        return args -> {
            log.info("Applying runtime configuration to Redis server...");
            try (RedisConnection conn = connectionFactory.getConnection()) {
                RedisServerCommands commands = conn.serverCommands();

                // Persistence - RDB snapshots
                commands.setConfig("save", "900 1 300 10 60 100");

                // AOF configuration
                commands.setConfig("appendonly", "yes");
                commands.setConfig("appendfsync", "everysec");
                commands.setConfig("auto-aof-rewrite-percentage", "100");
                commands.setConfig("auto-aof-rewrite-min-size", "64mb");

                // Memory management
                commands.setConfig("maxmemory", "512mb");
                commands.setConfig("maxmemory-policy", "allkeys-lru");

                // Network
                commands.setConfig("tcp-keepalive", "60");
                commands.setConfig("timeout", "300");

                // Logging
                commands.setConfig("loglevel", "notice");

                // Data structure optimization
                commands.setConfig("hash-max-ziplist-entries", "512");
                commands.setConfig("hash-max-ziplist-value", "64");
                commands.setConfig("list-max-ziplist-size", "-2");
                commands.setConfig("set-max-intset-entries", "512");
                commands.setConfig("zset-max-ziplist-entries", "128");
                commands.setConfig("zset-max-ziplist-value", "64");

                log.info("Redis server configured successfully");
            } catch (Exception e) {
                log.error("Failed to configure Redis server: {}", e.getMessage());
                throw e;
            }
        };
    }
}