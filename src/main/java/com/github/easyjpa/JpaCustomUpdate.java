
package com.github.easyjpa;

/**
 * 
 * Execute the CriteriaUpdate or CriteriaDelete built by a callback.
 * 
 * @Description: JpaCustomUpdate
 * @Author: Fred Feng
 * @Date: 18/10/2024
 * @Version 1.0.0
 */
public interface JpaCustomUpdate<X> {

    JpaUpdate<X> update(Class<X> entityClass);

    JpaDelete<X> delete(Class<X> entityClass);

    int executeUpdate(JpaDeleteCallback<X> callback);

    int executeUpdate(JpaUpdateCallback<X> callback);
}
