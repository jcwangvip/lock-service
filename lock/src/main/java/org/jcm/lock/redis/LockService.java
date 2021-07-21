package org.jcm.lock.redis;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jcm.lock.LockException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * 锁服务
 *
 * @author jcwang
 */
@Slf4j
@Component
@AllArgsConstructor
public class LockService {


    private final RedissonClient redissonClient;


    /**
     * 有返回值加锁
     *
     * @param supplier supplier
     * @param lockPath 加锁路径
     */
    public <R> R syncLock(Supplier<R> supplier, String lockPath) {
        return syncLock(supplier, Collections.singletonList(lockPath));
    }

    /**
     * 有返回值加锁
     *
     * @param supplier  supplier
     * @param lockPaths 所路径集合
     */
    public <R> R syncLock(Supplier<R> supplier, List<String> lockPaths) {
        if (lockPaths.isEmpty()) {
            log.info("警告:没有需要加锁的方法,不建议使用本方法调用");
            return supplier.get();
        }
        LockHelper lockHelper = LockHelper.builder().redissonClient(redissonClient).build();
        RLock rLock = lockHelper.getRlock(lockPaths);
        try {
            lockHelper.tryLock(lockPaths, rLock);
            return supplier.get();
        } catch (InterruptedException e) {
            log.error("加锁过程异常{0}", e);
            throw new LockException("加锁过程异常:" + e.getMessage());
        } finally {
            lockHelper.unlock(lockPaths, rLock);
        }
    }

    /**
     * 无返回值加锁
     *
     * @param runnable  可执行的线程
     * @param lockPaths 所路径集合
     */
    public void syncLock(Runnable runnable, List<String> lockPaths) {
        if (lockPaths.isEmpty()) {
            log.info("警告:没有需要加锁的方法,不建议使用本方法调用");
            runnable.run();
            return;
        }
        LockHelper lockHelper = LockHelper.builder().redissonClient(redissonClient).build();
        RLock rLock = lockHelper.getRlock(lockPaths);
        try {
            lockHelper.tryLock(lockPaths, rLock);
        } catch (InterruptedException e) {
            log.error("加锁过程异常{0}", e);
            throw new LockException("加锁过程异常: " + e.getMessage());
        } finally {
            lockHelper.unlock(lockPaths, rLock);
        }
    }

}
