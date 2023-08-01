package org.jcm.lock.redis;

import lombok.extern.slf4j.Slf4j;
import org.jcm.lock.LockException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * redis client
 *
 * @author jcwang
 */
@Slf4j
@Component
public class RedisLockOneClientImpl {

    @Autowired
    private RedissonClient redissonClient;

    public <R> R locks(Runnable runnable, Collection<String> lockPaths, long waitTime, long leaseTime) {
        String[] paths = (String[]) lockPaths.toArray();
        RLock[] locks = new RLock[paths.length];
        for (int i = 0; i < paths.length; ++i) {
            locks[i] = redissonClient.getLock(paths[i]);
        }
        try {
            boolean locked = false;
            while (!locked) {
                try {
                    locked = redissonClient.getMultiLock(locks).tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            boolean result = redissonClient.getMultiLock(locks).tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (!result) {
                throw new LockException("Failed to acquire distributed lock");
            }
            runnable.run();
        } catch (Throwable throwable) {
            throw new RuntimeException("Failed to process multi lock operation", throwable);
        } finally {
            redissonClient.getMultiLock(locks).unlock();
        }
        return null;
    }


}
