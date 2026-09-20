
package com.github.easyjpa;

/**
 * 
 * Thrown when a bean property can neither be read nor written.
 * 
 * @Description: PropertyAccessException
 * @Author: Fred Feng
 * @Date: 17/08/2026
 * @Version 1.0.0
 */
public class PropertyAccessException extends RuntimeException {

    private static final long serialVersionUID = 3120118845435303369L;

    public PropertyAccessException(String msg) {
        super(msg);
    }

    public PropertyAccessException(String msg, Throwable e) {
        super(msg, e);
    }

}
