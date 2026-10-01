
package com.github.easyjpa;

/**
 * 
 * JpaDelete represents a delete statement
 * 
 * @Author: Fred Feng
 * @Date: 18/10/2024
 * @Version 1.0.0
 */
public interface JpaDelete<E> extends Executable {

    JpaDelete<E> filter(Filter filter);

    <X> JpaSubQuery<X, X> subQuery(Class<X> entityClass);

    /** Name the table of the subquery, so that it and this statement tell theirs apart. */
    <X> JpaSubQuery<X, X> subQuery(Class<X> entityClass, String alias);

    <X, Y> JpaSubQuery<X, Y> subQuery(Class<X> entityClass, Class<Y> resultClass);

    <X, Y> JpaSubQuery<X, Y> subQuery(Class<X> entityClass, String alias, Class<Y> resultClass);

}
