package org.jcm.lock;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
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

    private final LockClient client;


    /**
     * 加锁
     *
     * @param supplier  supplier
     * @param lockPaths 锁路径
     * @param <R>       R
     * @return R
     * @throws InterruptedException
     */
    public <R> R call(Supplier<R> supplier, Collection<String> lockPaths) throws InterruptedException {
        if (CollectionUtils.isEmpty(lockPaths)) {
            log.debug("lockPaths is empty.");
            return supplier.get();
        }
        return client.lock(supplier, lockPaths, 10, 600);
    }


    /**
     * 加锁
     * 可以自定义等待时间和过期时间,单位为秒
     *
     * @param supplier  supplier
     * @param lockPaths 锁路径
     * @param waitTime  等待时间
     * @param leaseTime 过期时间
     * @param <R>       R
     * @return R
     * @throws InterruptedException
     */
    public <R> R call(Supplier<R> supplier, Collection<String> lockPaths, long waitTime, long leaseTime) throws InterruptedException {
        if (CollectionUtils.isEmpty(lockPaths)) {
            log.debug("lockPaths is empty.");
            return supplier.get();
        }
        return client.lock(supplier, lockPaths, waitTime, leaseTime);
    }


}
