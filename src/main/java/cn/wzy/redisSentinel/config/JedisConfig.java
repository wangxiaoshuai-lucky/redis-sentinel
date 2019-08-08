package cn.wzy.redisSentinel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Collections;

/**
 * @author `wangzy`
 * @version 1.0
 * @since 2019/8/7 14:46
 **/
@Configuration
public class JedisConfig {

    @Value("${redis.maxIdle}")
    private Integer maxIdle;

    @Value("${redis.maxActive}")
    private Integer maxActive;

    @Value("${redis.maxWait}")
    private Long maxWait;

    @Value("${redis.testOnBorrow}")
    private Boolean testOnBorrow;

    @Value("${redis.host}")
    private String host;

    @Value("${redis.port}")
    private int port;

    @Value("${redis.sentinel.port}")
    private Integer sentinelPort;

    @Bean
    public RedisSentinelConfiguration redisSentinelConfiguration() {
        RedisSentinelConfiguration configuration = new RedisSentinelConfiguration();
        RedisNode redisNode = new RedisNode(host, port);
        redisNode.setName("mymaster");
        configuration.setMaster(redisNode);
        configuration.setSentinels(Collections.singleton(new RedisNode(host, sentinelPort)));
        return configuration;
    }

    @Bean
    public JedisPoolConfig jedisPoolConfig() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxIdle(maxIdle);
        config.setMaxWaitMillis(maxWait);
        config.setMaxTotal(maxActive);
        config.setTestOnBorrow(testOnBorrow);
        return config;
    }

    @Bean
    public JedisConnectionFactory jedisConnectionFactory(JedisPoolConfig jedisPoolConfig,
                                                         RedisSentinelConfiguration redisSentinelConfiguration) {
        return new JedisConnectionFactory(redisSentinelConfiguration, jedisPoolConfig);
    }

    @Bean
    public StringRedisTemplate redisTemplate(
            JedisConnectionFactory jedisConnectionFactory) {
        StringRedisTemplate redisTemplate = new StringRedisTemplate();
        redisTemplate.setConnectionFactory(jedisConnectionFactory);
        return redisTemplate;
    }
}
