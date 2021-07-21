package org.jcm.lock;

import lombok.Getter;

/**
 * 类描述
 *
 * @author jcwang
 */
public class LockException extends RuntimeException {

    @Getter
    private final Object[] args;

    public LockException(String msg, Object... args) {
        super(msg);
        this.args = args;
    }
}
