package org.jcm.lock;

/**
 * 锁对象
 * 面向对象加锁的方式都需要实现本类
 *
 * @author jcwang
 */
public interface LockObject {

    /**
     * 需要加锁的路径
     *
     * @return 字符串
     */
    String lockPath();
}
