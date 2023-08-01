package org.jcm.lock.redis;

import lombok.extern.slf4j.Slf4j;
import org.jcm.lock.LockException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * redis client
 *
 * @author jcwang
 */
@Slf4j
@Component
public class RedisLockTwoClientImpl {

    @Autowired
    private RedissonClient redissonClient;

    /*

    代码中可能存在的问题和可以进行优化的地方如下：

    可能存在死锁问题：在获取锁的过程中，如果某个线程获取了部分锁，而其他线程也在尝试获取相同的部分锁，那么就会出现死锁问题。为了避免这种情况，可以在获取锁时指定锁的顺序，以此来避免死锁的出现。

    可能存在锁竞争问题：在高并发环境下，大量线程同时获取锁时，可能会出现锁竞争的情况，导致性能下降。为了避免这种情况，可以考虑将锁的粒度更细化，将单个锁拆分为多个小锁，以此来提高并发性能。

    可能存在锁泄漏问题：在释放锁的过程中可能会出现异常，导致锁没有被正确地释放，从而导致锁泄漏问题。为了避免这种情况，可以采用基于AOP技术的方式，对锁操作进行切面拦截，以确保在任何情况下都能够正确地释放锁。

    可以缩短锁的等待时间：在获取锁的过程中，可以通过调整等待时间和租约时间的值，来优化锁的性能和响应速度，从而提高系统的吞吐量和响应能力。

    可以使用RedLock机制：为了提高分布式锁的可靠性和健壮性，可以使用RedLock机制，即在不同的Redis节点上获取锁，以此来避免单点故障和数据丢失问题。

    可以使用Redis Cluster：为了提高Redis的性能和可扩展性，可以考虑使用Redis Cluster集群，以支持大规模并发访问和数据存储，并通过集群间的数据复制和负载均衡来提高系统的可靠性和稳定性。

    */

    /*关于具体的代码优化方案如下：

    加锁顺序优化：为了避免死锁问题，我们可以在获取锁时指定锁的顺序，例如按照路径的字典序来获取锁。具体代码实现如下：
            Arrays.sort(lockPaths);
    for (String lockPath : lockPaths) {
            RLock lock = redissonClient.getLock(lockPath);
            multiLock.add(lock);
        }
    锁粒度优化：为了提高并发性能，我们可以将单个锁拆分为多个小锁，以便更多的线程可以并发地执行操作。具体代码实现如下：
            for (String lockPath : lockPaths) {
        RLock lock = redissonClient.getLock(lockPath + ":" + Thread.currentThread().getName());
        multiLock.add(lock);
    }
    锁的释放优化：为了避免锁泄漏问题，我们使用AOP技术对锁的操作进行切面拦截，在释放锁的过程中，即使出现了异常，也能够确保锁能够正确地被释放。具体代码实现如下：
    @Around("@annotation(com.example.lock.annotation.RedisLock)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        // 获取方法注解信息
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        RedisLock redisLock = method.getAnnotation(RedisLock.class);
        // 获取锁的路径、租约时间和等待时间等信息
        String[] lockPaths = redisLock.lockPaths();
        int leaseTime = redisLock.leaseTime();
        int waitTime = redisLock.waitTime();
        // 获取多重锁对象
        RLock[] locks = new RLock[lockPaths.length];
        for (int i = 0; i < lockPaths.length; i++) {
            String lockPath = lockPaths[i] + ":" + Thread.currentThread().getName();
            RLock lock = redissonClient.getLock(lockPath);
            locks[i] = lock;
        }
        RLock multiLock = redissonClient.getMultiLock(locks);
        // 尝试获取多重锁
        boolean locked = multiLock.tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS);
        if (!locked) {
            throw new LockException("Failed to acquire lock!");
        }
        try {
            // 执行业务逻辑
            return point.proceed();
        } finally {
            // 释放多重锁和单个锁
            multiLock.unlock();
            for (RLock lock : locks) {
                lock.unlock();
            }
        }
    }
    等待时间和租约时间参数优化：为了提供更好的性能和响应能力，我们可以将等待时间和租约时间的值根据实际需求进行调整。例如，在低压力的情况下，可以将等待时间和租约时间缩短，以快速地完成锁的操作。具体代码实现如下：
    // 尝试获取多重锁
    int waitTimeMillis = Math.min(waitTime, 50);
    int leaseTimeMillis = Math.min(leaseTime, 50);
    boolean locked = multiLock.tryLock(waitTimeMillis, leaseTimeMillis, TimeUnit.MILLISECONDS);
if (!locked) {
        throw new LockException("Failed to acquire lock!");
    }
    RedLock机制优化：为了提高分布式锁的可靠性和健壮性，可以使用RedLock机制，即在不同的Redis节点上获取锁。具体代码实现如下：
    // 获取Redis集群节点信息
    List<String> clusterNodes = redissonProperties.getClusterNodes();
    List<URI> uriList = new ArrayList<>();
for (String node : clusterNodes) {
        uriList.add(URI.create("redis://" + node));
    }

    // 获取RedLock对象
    RedissonRedLock redLock = new RedissonRedLock(locks);
    boolean locked = redLock.tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS);
if (!locked) {
        throw new LockException("Failed to acquire lock!");
    }
try {
        // 执行业务逻辑
        return point.proceed();
    } finally {
        // 释放RedLock
        redLock.unlock();
    }
    Redis Cluster优化：为了提高Redis的性能和可扩展性，可以使用Redis Cluster集群。可以将多个Redis节点组成一个集群，通过在集群间进行数据复制和负载均衡，实现高可用性和高性能的分布式锁服务。具体配置方式可以参考Redis官方文档。
    */


    public <R> R locks(Callable<R> callable, Set<String> lockPaths, long waitTime, long leaseTime) throws LockException, ExecutionException {
        Arrays.sort(new Set[]{lockPaths});
        String[] paths = lockPaths.toArray(new String[lockPaths.size()]);
        RLock[] locks = new RLock[paths.length];
        for (int i = 0; i < paths.length; ++i) {
            locks[i] = redissonClient.getLock(paths[i]);
        }
        RLock multiLock = redissonClient.getMultiLock(locks);
        try {
            boolean locked = false;
            while (!locked) {
                locked = multiLock.tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS);
                if (!locked) {
                    log.warn("Failed to acquire multi lock({})", Arrays.toString(paths));
                }
            }

            if (multiLock.isLocked()) {
                return callable.call();
            } else {
                throw new LockException("Failed to acquire multi lock(" + Arrays.toString(paths) + ")");
            }
            // 可以选择具体的异常类型
        } catch (TimeoutException | InterruptedException e) {
            throw new LockException(e.getMessage(), e);
        } catch (Exception e) {
            throw new ExecutionException(e.getMessage(), e);
        } finally {
            try {
                if (multiLock.isHeldByCurrentThread()) {
                    multiLock.unlock();
                }
            } catch (Exception e) {
                // 可以记录日志或者抛出异常
                log.error("Failed to release multi lock({}): {}", Arrays.toString(paths), e.getMessage(), e);
            }
            for (RLock lock : locks) {
                try {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                } catch (Exception e) {
                    // 可以记录日志或者抛出异常
                    log.error("Failed to release lock({}): {}", lock.getName(), e.getMessage(), e);
                }
            }
        }
    }

}
