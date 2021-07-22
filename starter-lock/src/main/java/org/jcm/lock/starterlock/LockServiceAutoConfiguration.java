package org.jcm.lock.starterlock;


import org.jcm.lock.redis.RedisService;
import org.jcm.lock.redis.ScheduledLockInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * lockServiceAutoConfiguration
 *
 * @author jcwang
 */
@Configuration
@EnableConfigurationProperties(LockProperties.class)
public class LockServiceAutoConfiguration {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Bean
    public ScheduledLockInterceptor scheduledLockInterceptor() {
        return new ScheduledLockInterceptor(new RedisService(redisTemplate));
    }

}
