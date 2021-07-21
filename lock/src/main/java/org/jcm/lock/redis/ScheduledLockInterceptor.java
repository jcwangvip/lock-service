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
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 定时任务锁拦截器
 *
 * @author jcwang
 */
@Aspect
@Slf4j
@Component
@AllArgsConstructor
public class ScheduledLockInterceptor {


    private final RedisService redisService;

    @Pointcut("@annotation(org.jcm.lock.ScheduledLocked)")
    public void scheduledLockedPointcut() {
    }

    @Around("scheduledLockedPointcut()")
    public Object scheduledLocked(ProceedingJoinPoint pjp) throws Throwable {
        boolean debugEnabled = log.isDebugEnabled();
        if (debugEnabled) {
            log.debug("进入scheduledLocked流程...");
        }
        Signature signature = pjp.getSignature();
        String key = getScheduledLockedName(signature);
        try {
            Boolean isSuccess = redisService.scheduledLook(key, key);
            if (!isSuccess) {
                if (debugEnabled) {
                    log.debug("{},{} 已被锁定,终止执行", key, key);
                }
                return null;
            }
            return pjp.proceed();
        } catch (InterruptedException e) {
            log.error("scheduledLocked加锁过程异常{0}", e);
            throw new LockException("加锁过程异常:" + e.getMessage());
        } catch (Throwable throwable) {
            log.error("scheduledLocked加锁后,目标方法执行异常{0}", throwable);
            throw throwable;
        } finally {
            redisService.scheduledUnLook(key);
        }
    }

    private String getScheduledLockedName(Signature signature) {
        MethodSignature methodSignature = (MethodSignature) signature;
        Method targetMethod = methodSignature.getMethod();
        ScheduledLocked scheduledLocked = targetMethod.getAnnotation(ScheduledLocked.class);
        if (StringUtils.isEmpty(scheduledLocked.name())) {
            return "scheduledLocked#" + signature.getName();
        }
        return scheduledLocked.name();
    }



}
