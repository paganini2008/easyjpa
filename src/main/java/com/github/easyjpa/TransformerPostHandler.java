
package com.github.easyjpa;

/**
 * 
 * Called after a transformation to touch up the destination object.
 * 
 * @Description: TransformerPostHandler
 * @Author: Fred Feng
 * @Date: 18/10/2024
 * @Version 1.0.0
 */
@FunctionalInterface
public interface TransformerPostHandler<T, R> {

    void handleAfterTransformation(Model<?> model, T original, R destination);

}
