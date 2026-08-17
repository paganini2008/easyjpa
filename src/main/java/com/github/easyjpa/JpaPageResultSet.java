
package com.github.easyjpa;

import com.github.easyjpa.page.Countable;
import com.github.easyjpa.page.PageableQuery;

/**
 * 
 * Result set of a pagination.
 * 
 * @Description: JpaPageResultSet
 * @Author: Fred Feng
 * @Date: 18/10/2024
 * @Version 1.0.0
 */
public interface JpaPageResultSet<T> extends ListableQuery<T>, Countable {

    <R> PageableQuery<R> setTransformer(Transformer<T, R> transformer);

}
