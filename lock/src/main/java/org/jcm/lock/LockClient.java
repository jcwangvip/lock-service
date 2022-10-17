package org.jcm.lock;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * 锁客户端
 *
 * @author jcwang
 */
public interface LockClient {

    /**
     * 锁
     *
     * @param runnable  runnable
     * @param lockPaths 锁的路径
     * @param waitTime  等待时间
     * @param leaseTime 过期时间
     * @return R
     * @throws InterruptedException 释放锁失败异常
     */
    void lock(Runnable runnable, Collection<String> lockPaths, long waitTime, long leaseTime) throws InterruptedException;

    /**
     * 锁
     *
     * @param supplier  supplier
     * @param lockPaths 锁的路径
     * @param waitTime  等待时间
     * @param leaseTime 过期时间
     * @param <R>       R
     * @return R
     * @throws InterruptedException 释放锁失败异常
     */
    <R> R lock(Supplier<R> supplier, Collection<String> lockPaths, long waitTime, long leaseTime) throws InterruptedException;

    /**
     * 锁
     *
     * @param supplier  supplier
     * @param lockPaths 锁的路径
     * @param waitTime  等待时间
     * @param leaseTime 过期时间
     * @param <R>       R
     * @return R
     */
    <R> R locks(Supplier<R> supplier, Collection<String> lockPaths, long waitTime, long leaseTime);
}
