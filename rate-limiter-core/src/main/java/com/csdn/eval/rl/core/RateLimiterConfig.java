package com.csdn.eval.rl.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
@ComponentScan({"com.csdn.eval.rl.**"})
public class RateLimiterConfig {

    @Autowired
    private RateLimiterRedisProperties properties;

    @Bean("evalRateLimiterRedisTemplate")
    public RedisTemplate<String, String> redisTemplate() {
        JedisConnectionFactory jedisConnectionFactory = this.getJedisConnectionFactory();
        RedisTemplate<String, String> template = new RedisTemplate<>();
        // 设置key的序列化方式
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setConnectionFactory(jedisConnectionFactory);
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        // 设置Hash的key和value序列化方式
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);
        // 设置value的泛型类型，这样在存取的时候才会序列化和反序列化成设置的对象类型
        // 注意：这里只是设置了value的泛型，key还是String类型
        template.afterPropertiesSet();
        return template;
    }

    private JedisConnectionFactory getJedisConnectionFactory() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        // 设置连接池参数，例如最大连接数、最大空闲连接数等
        poolConfig.setMaxTotal(100);
        poolConfig.setMaxIdle(30);
        poolConfig.setMinIdle(10);

        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory(poolConfig);
        jedisConnectionFactory.setHostName(properties.getHost());
        jedisConnectionFactory.setPort(properties.getPort());
        jedisConnectionFactory.setDatabase(properties.getDatabase());
        jedisConnectionFactory.setPassword(properties.getPassword());
        jedisConnectionFactory.afterPropertiesSet();
        return jedisConnectionFactory;
    }
}
