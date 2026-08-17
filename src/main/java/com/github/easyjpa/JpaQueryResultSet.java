
package com.github.easyjpa;

import com.github.easyjpa.page.Countable;

/**
 * 
 * Result set of a query.
 * 
 * @Description: JpaQueryResultSet
 * @Author: Fred Feng
 * @Date: 18/10/2024
 * @Version 1.0.0
 */
public interface JpaQueryResultSet<T> extends ListableQuery<T>, Countable {

    <R> ListableQuery<R> setTransformer(Transformer<T, R> transformer);

    T one();

}
