package org.jcm.lock.redis;

import lombok.extern.slf4j.Slf4j;
import org.jcm.lock.LockClient;
import org.jcm.lock.LockException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * redis client
 *
 * @author jcwang
 */
@Slf4j
@Component
public class RedisLockClientImpl implements LockClient {

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public void lock(Runnable runnable, Collection<String> lockPaths, long waitTime, long leaseTime) throws InterruptedException {
        List<RLock> rLocks = lockPaths.stream().distinct().map(redissonClient::getLock).collect(Collectors.toList());
        RLock[] locks = new RLock[rLocks.size()];
        for (int i = 0; i < rLocks.size(); i++) {
            locks[i] = rLocks.get(i);
        }
        List<String> lockPathList = rLocks.stream().map(RLock::getName).collect(Collectors.toList());
        log.info("current lock path {}", lockPathList);
        RLock multiLock = redissonClient.getMultiLock(locks);
        try {
            boolean lockSuccess = multiLock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (!lockSuccess) {
                log.error("current lock path time out : {}", lockPaths);
                throw new LockException("current lock path time out");
            }
            runnable.run();
        } catch (LockException e) {
            log.error("current lock path lockException,{}, {}", lockPaths, e);
            throw e;
        } catch (Exception e) {
            log.error("current lock path exception,{}, {}", lockPaths, e);
            throw e;
        } finally {
            try {
                multiLock.unlock();
            } catch (Exception e) {
                log.error("current lock path unlock exception:", e);
            }
        }
    }

    @Override
    public <R> R lock(Supplier<R> supplier, Collection<String> lockPaths, long waitTime, long leaseTime) throws InterruptedException {
        List<RLock> rLocks = lockPaths.stream().distinct().map(redissonClient::getLock).collect(Collectors.toList());
        RLock[] locks = new RLock[rLocks.size()];
        for (int i = 0; i < rLocks.size(); i++) {
            locks[i] = rLocks.get(i);
        }
        List<String> lockPathList = rLocks.stream().map(RLock::getName).collect(Collectors.toList());
        log.info("current lock path {}", lockPathList);
        RLock multiLock = redissonClient.getMultiLock(locks);
        try {
            boolean lockSuccess = multiLock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (!lockSuccess) {
                log.error("current lock path time out : {}", lockPaths);
                throw new LockException("current lock path time out");
            }
            return supplier.get();
        } catch (LockException e) {
            log.error("current lock path lockException,{}, {}", lockPaths, e);
            throw e;
        } catch (Exception e) {
            log.error("current lock path exception,{}, {}", lockPaths, e);
            throw e;
        } finally {
            try {
                multiLock.unlock();
            } catch (Exception e) {
                log.error("current lock path unlock exception:", e);
            }
        }
    }

    @Override
    public void locks(Runnable runnable, Collection<String> lockPaths, long waitTime, long leaseTime) {
        RLock multiLock = getLock(lockPaths);
        // 这里的InterruptedException是不是需要捕获后转换成别的异常
        boolean lockSuccess;
        try {
            lockSuccess = multiLock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // 锁异常是声明好,还是运行时好
            throw new LockException("current lock path InterruptedException {} ", lockPaths);
        }
        if (lockSuccess) {
            try {
                runnable.run();
            } catch (Exception e) {
                log.error("current lock execute business exception, {}, {}", lockPaths, e);
                throw e;
            } finally {
                log.info("current lock path unlock, {}", lockPaths);
                try {
                    multiLock.unlock();
                } catch (Exception e) {
                    log.error("current lock path unlock exception: {} , {}", lockPaths, e);
                }
            }
        } else {
            log.info("current lock path tryLockFail : {}", lockPaths);
            throw new LockException("current lock path tryLockFail");
        }
    }


    @Override
    public <R> R locks(Supplier<R> supplier, Collection<String> lockPaths, long waitTime, long leaseTime) {
        RLock multiLock = getLock(lockPaths);
        // 这里的InterruptedException是不是需要捕获后转换成别的异常
        boolean lockSuccess;
        try {
            lockSuccess = multiLock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // 锁异常是声明好,还是运行时好
            throw new LockException("current lock path InterruptedException {} ", lockPaths);
        }
        if (lockSuccess) {
            try {
                return supplier.get();
            } catch (Exception e) {
                log.error("current lock execute business exception, {}, {}", lockPaths, e);
                throw e;
            } finally {
                log.info("current lock path unlock, {}", lockPaths);
                try {
                    multiLock.unlock();
                } catch (Exception e) {
                    log.error("current lock path unlock exception: {} , {}", lockPaths, e);
                }
            }
        } else {
            log.info("current lock path tryLockFail : {}", lockPaths);
            throw new LockException("current lock path tryLockFail");
        }
    }

    private RLock getLock(Collection<String> lockPaths) {
        List<RLock> rLocks = lockPaths.stream().distinct().map(redissonClient::getLock).collect(Collectors.toList());
        RLock[] locks = new RLock[rLocks.size()];
        for (int i = 0; i < rLocks.size(); i++) {
            locks[i] = rLocks.get(i);
        }
        List<String> lockPathList = rLocks.stream().map(RLock::getName).collect(Collectors.toList());
        log.info("current lock path {}", lockPathList);
        return redissonClient.getMultiLock(locks);
    }


}
