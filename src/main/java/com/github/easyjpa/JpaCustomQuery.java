
package com.github.easyjpa;

import java.util.List;

/**
 * 
 * Execute the CriteriaQuery built by a callback.
 * 
 * @Description: JpaCustomQuery
 * @Author: Fred Feng
 * @Date: 18/10/2024
 * @Version 1.0.0
 */
public interface JpaCustomQuery<X> {

    <T> T getSingleResult(JpaQueryCallback<T> callback);

    <T> List<T> getResultList(JpaQueryCallback<T> callback);

    <T> List<T> getResultList(JpaQueryCallback<T> callback, int maxResults, long firstResult);
}
