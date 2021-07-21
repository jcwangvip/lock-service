package org.jcm.lock.redis;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 类描述
 *
 * @author jcwang
 */
@Slf4j
@Service
@AllArgsConstructor
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;


    public Boolean scheduledLook(String k, String v) {
        return redisTemplate.opsForValue().setIfAbsent(k, v);
    }

    public Boolean scheduledLook(String k, String v, long timeOut) {
        return redisTemplate.opsForValue().setIfAbsent(k, v, timeOut, TimeUnit.SECONDS);
    }

    public Boolean scheduledUnLook(String k) {
        return redisTemplate.delete(k);
    }


}
