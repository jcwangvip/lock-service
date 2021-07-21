package org.jcm.lock.boot;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 类描述
 *
 * @author jcwang
 */
@Slf4j
@SpringBootTest
public class RedissonApiTest {

    @Autowired
    private RedissonClient redissonClient;

    @Test
    void getMultiLockTest1() {
        String lockPath = "test1";
        String lockPath2 = "test2";
        List<String> lockPaths = Arrays.asList(lockPath, lockPath2);
        RLock[] rLocks = lockPaths.stream()
                .map(redissonClient::getLock).toArray(RLock[]::new);
        RLock rLock = redissonClient.getMultiLock(rLocks);
        try {
            boolean lockResult = rLock.tryLock(30, 600, TimeUnit.SECONDS);
            if (!lockResult) {
                log.info("加锁失败:锁路径 {}", lockPath);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    void getMultiLockTest() {
        String lockPath = "test1";
        String lockPath2 = "test2";
        List<String> lockPaths = Arrays.asList(lockPath, lockPath2);
        List<RLock> rLockList = lockPaths.stream()
                .map(redissonClient::getLock).collect(Collectors.toList());
        RLock[] rLocks = new RLock[rLockList.size()];
        for (int i = 0; i < rLockList.size(); i++) {
            rLocks[i] = rLockList.get(i);
        }

        RLock rLock = redissonClient.getMultiLock(rLocks);
        try {
            boolean lockResult = rLock.tryLock(30, 600, TimeUnit.SECONDS);
            if (!lockResult) {
                log.info("加锁失败:锁路径 {}", lockPath);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    void getLockTest() {
        String lockPath = "test";
        RLock rLock = redissonClient.getLock(lockPath);
        try {
            boolean lockResult = rLock.tryLock(30, 600, TimeUnit.SECONDS);
            if (!lockResult) {
                log.info("加锁失败:锁路径 {}", lockPath);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
