package com.csdn.eval.rl.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

/**
 * 基于Redis的分布式限流器实现
 *
 * @author: xiewenfeng
 */
@Component
public class RateLimiter implements EnvironmentAware {

    private static final Logger logger = LoggerFactory.getLogger(RateLimiter.class);
    private static final String RATE_LIMITER_KEY_PREFIX = "eval:rate_limiter";

    private final RedisTemplate<String, String> redisTemplate;
    private final RateLimiterRedisProperties properties;
    private final ValueOperations<String, String> valueOperations;

    private Environment environment;
    private String rateLimiterKey;

    @Autowired
    public RateLimiter(
            @Qualifier("evalRateLimiterRedisTemplate") RedisTemplate<String, String> redisTemplate,
            RateLimiterRedisProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.valueOperations = redisTemplate.opsForValue();
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void init() {
        this.rateLimiterKey = generateRateLimiterKey();
        logger.info("RateLimiter initialized with key: {}", rateLimiterKey);
    }

    /**
     * 尝试获取限流通行证
     *
     * @return true表示允许通过，false表示被限流
     */
    public synchronized boolean tryAcquire() {
        if (!properties.getEnable()) {
            return true;
        }
        try {
            int currentCount = getCurrentRequestCount();
            if (currentCount >= properties.getMaxCount()) {
                logger.warn("Rate limit exceeded - window: {} minutes, max: {}, current: {}",
                        properties.getWindowMinutes(),
                        properties.getMaxCount(),
                        currentCount);
                return false;
            }
            incrementRequestCount();
            return true;
        } catch (Exception e) {
            logger.error("Failed to acquire rate limit token for key: {}", rateLimiterKey, e);
            // 在限流器异常时默认放行，保证系统可用性
            return true;
        }
    }

    /**
     * 获取当前窗口的请求计数
     */
    public int getCurrentRequestCount() {
        try {
            String countStr = valueOperations.get(rateLimiterKey);
            return countStr != null ? Integer.parseInt(countStr) : 0;
        } catch (Exception e) {
            logger.error("Failed to get current request count for key: {}", rateLimiterKey, e);
            return 0;
        }
    }

    private void incrementRequestCount() {
        Long count = valueOperations.increment(rateLimiterKey, 1);
        if (count != null && count == 1) {
            // 首次设置时初始化过期时间
            redisTemplate.expire(rateLimiterKey, properties.getWindowMinutes(), TimeUnit.MINUTES);
        }
    }

    /**
     * 生成限流器的唯一键,基于主机地址和端口号（这样可以达到分布式限流效果，对不同的应用进行限流）
     */
    private String generateRateLimiterKey() {
        String port = environment.getProperty("server.port", "unknown");
        String host = getLocalHostAddress();
        return String.format("%s:%s:%s", RATE_LIMITER_KEY_PREFIX, host, port);
    }

    private String getLocalHostAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            logger.warn("Failed to get local host address, using 127.0.0.1 as fallback", e);
            return "127.0.0.1";
        }
    }

    public int getMaxCount() {
        return properties.getMaxCount();
    }

    /**
     * 获取剩余的请求次数
     */
    public int getRemainingRequests() {
        return Math.max(0, properties.getMaxCount() - getCurrentRequestCount());
    }
}