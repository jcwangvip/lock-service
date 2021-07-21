package org.jcm.lock.redis;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.jcm.lock.LockException;
import org.jcm.lock.ScheduledLocked;
import org.jcm.lock.redis.LockHelper;
import org.jcm.lock.redis.RedisService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 锁拦截器
 *
 * @author jcwang
 */
@Aspect
@Slf4j
@Component
@AllArgsConstructor
public class LockInterceptor {


    private final RedissonClient redissonClient;

    @Pointcut("@annotation(org.jcm.lock.Locked) ")
    public void lockPointcut() {
    }

    @Around("lockPointcut()")
    public Object lock(ProceedingJoinPoint pjp) throws Throwable {
        boolean debugEnabled = log.isDebugEnabled();
        if (debugEnabled) {
            log.debug("进入lock流程...");
        }
        LockHelper lockHelper = LockHelper.builder().redissonClient(redissonClient).build();
        List<String> lockPaths = lockHelper.getPath(pjp);
        RLock rLock = lockHelper.getRlock(lockPaths);
        try {
            lockHelper.tryLock(lockPaths, rLock);
            return pjp.proceed();
        } catch (InterruptedException e) {
            log.error("加锁过程异常{0}", e);
            throw new LockException("加锁过程异常:" + e.getMessage());
        } catch (Throwable throwable) {
            log.error("加锁后,目标方法执行异常{0}", throwable);
            throw throwable;
        } finally {
            lockHelper.unlock(lockPaths, rLock);
        }
    }


}
