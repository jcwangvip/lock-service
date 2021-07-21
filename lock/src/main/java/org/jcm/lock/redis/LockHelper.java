package org.jcm.lock.redis;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.jcm.lock.LockException;
import org.jcm.lock.LockObject;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 锁helper
 *
 * @author jcwang
 */
@Slf4j
@Builder
public class LockHelper {


    final RedissonClient redissonClient;


    protected void tryLock(List<String> lockPaths, RLock rLock) throws InterruptedException {
        log.info("开始加锁,锁路径数量 = {},值 = {}", lockPaths.size(), lockPaths);
        boolean lockResult = rLock.tryLock(30, 600, TimeUnit.SECONDS);
        if (!lockResult) {
            log.error("加锁失败:锁路径 {}", lockPaths);
            throw new LockException("加锁失败" + lockPaths);
        }
        log.info("开始加锁成功,锁路径数量 = {},值 = {}", lockPaths.size(), lockPaths);
    }

    protected RLock getRlock(List<String> lockPaths) {
        long count = lockPaths.stream().filter(StringUtils::isEmpty).count();
        if (count > 0) {
            throw new LockException("锁路径中不能包含null");
        }
        RLock[] rLocks = lockPaths.stream()
                .map(redissonClient::getLock)
                .toArray(RLock[]::new);
        return redissonClient.getMultiLock(rLocks);
    }

    protected List<String> getPath(ProceedingJoinPoint pjp) {
        List<String> lockPathList = new ArrayList<>();
        Object[] args = pjp.getArgs();
        for (Object arg : args) {
            addPath(pjp, lockPathList, arg);
            if (arg instanceof Collection) {
                Collection<?> collection = (Collection<?>) arg;
                for (Object o : collection) {
                    addPath(pjp, lockPathList, o);
                }
            }
        }
        if (CollectionUtils.isEmpty(lockPathList)) {
            throw new LockException("方法[" + pjp.getSignature().getName() + "]没有找到可用的锁路径");
        }
        return lockPathList;
    }

    private void addPath(ProceedingJoinPoint pjp, List<String> lockPathList, Object arg) {
        if (arg instanceof LockObject) {
            LockObject lockObject = (LockObject) arg;
            String lockPath = lockObject.lockPath();
            if (StringUtils.isEmpty(lockPath)) {
                throw new LockException("方法 [" + pjp.getSignature().getName() + "] lockPath 不能使用 null");
            }
            lockPathList.add(lockPath);
        }
    }


    public void unlock(List<String> lockPaths, RLock rLock) {
        log.info("方法执行完毕,释放锁路径数量 = {},值 = {}", lockPaths.size(), lockPaths);
        rLock.unlock();
    }
}
