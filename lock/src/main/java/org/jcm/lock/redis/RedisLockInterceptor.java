package org.jcm.lock.redis;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.jcm.lock.LockException;
import org.jcm.lock.LockObject;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * redis client
 *
 * @author jcwang
 */
@Aspect
@Component
@Slf4j
@Order(-10000)
public class RedisLockInterceptor {

    private final RedissonClient redissonClient;

    public RedisLockInterceptor(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }


    @Pointcut("@annotation(org.jcm.lock.Locked)")
    public void lockedPointcut() {
    }



    private List<RLock> collectLockPath(ProceedingJoinPoint pjp) {
        List<RLock> objects = new ArrayList<>();
        for (int i = 0; i < pjp.getArgs().length; i++) {
            Object arg = pjp.getArgs()[i];
            if (arg instanceof LockObject) {
                LockObject lockObject = (LockObject) arg;
                RLock rLock = redissonClient.getLock(lockObject.lockPath());
                objects.add(rLock);
            }
            if (arg instanceof Collection) {
                Collection collection = (Collection) arg;
                for (Object obj : collection) {
                    if (obj instanceof LockObject) {
                        LockObject lockObject = (LockObject) obj;
                        RLock lock1 = redissonClient.getLock(lockObject.lockPath());
                        objects.add(lock1);
                    }
                }
            }
        }
        return objects;
    }

    @Around("lockedPointcut()")
    Object executeInLock(ProceedingJoinPoint pjp) throws Throwable {
        List<RLock> lockObjects = collectLockPath(pjp);
        if (lockObjects.isEmpty()) {
            log.warn("lockPaths is empty.");
            return pjp.proceed();
        }
        RLock[] rLocks = new RLock[lockObjects.size()];
        for (int i = 0; i < lockObjects.size(); i++) {
            rLocks[i] = lockObjects.get(i);
        }
        List<String> lockPaths = Arrays.stream(rLocks).map(RLock::getName).collect(Collectors.toList());
        log.info("current lock path {}", lockPaths);
        RLock multiLock = redissonClient.getMultiLock(rLocks);
        List<String> lockNames = lockObjects.stream().map(RLock::getName).collect(Collectors.toList());
        try {
            boolean lockSuccess = multiLock.tryLock(30, 600, TimeUnit.SECONDS);
            if (!lockSuccess) {
                log.error("current lock path time out : {}", lockNames);
                throw new LockException("current lock path time out");
            }
            return pjp.proceed();
        } catch (LockException e) {
            log.error("current lock path lockException,{}, {}", lockPaths, e);
            throw e;
        } catch (Exception e) {
            log.error("current lock path exception,{}, {}", lockPaths, e);
            throw e;
        } finally {
            try {
                multiLock.unlock();
            } catch (IllegalMonitorStateException e) {
                log.error("current lock path unlock exception:", e);
            }
        }
    }

}
