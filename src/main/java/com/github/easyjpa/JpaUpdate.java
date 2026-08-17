package com.github.easyjpa;

import com.github.easyjpa.LambdaUtils.LambdaInfo;

/**
 * 
 * JpaUpdate represents a update statement
 * 
 * @Author: Fred Feng
 * @Date: 07/10/2024
 * @Version 1.0.0
 */
public interface JpaUpdate<E> extends Executable {

    JpaUpdate<E> filter(Filter filter);

    default JpaUpdate<E> set(SerializableFunction<E, ?> function, Object value) {
        LambdaInfo lambdaInfo = LambdaUtils.inspect(function);
        return set(lambdaInfo.getAttributeName(), value);
    }

    default JpaUpdate<E> set(SerializableFunction<E, ?> function1, Object value1,
                             SerializableFunction<E, ?> function2, Object value2) {
        LambdaInfo lambdaInfo1 = LambdaUtils.inspect(function1);
        LambdaInfo lambdaInfo2 = LambdaUtils.inspect(function2);
        return set(lambdaInfo1.getAttributeName(), value1).set(lambdaInfo2.getAttributeName(),
                value2);
    }

    default JpaUpdate<E> set(SerializableFunction<E, ?> function1, Object value1,
                             SerializableFunction<E, ?> function2, Object value2, SerializableFunction<E, ?> function3,
                             Object value3) {
        LambdaInfo lambdaInfo1 = LambdaUtils.inspect(function1);
        LambdaInfo lambdaInfo2 = LambdaUtils.inspect(function2);
        LambdaInfo lambdaInfo3 = LambdaUtils.inspect(function3);
        return set(lambdaInfo1.getAttributeName(), value1)
                .set(lambdaInfo2.getAttributeName(), value2)
                .set(lambdaInfo3.getAttributeName(), value3);
    }

    /** Set the attribute to a literal value. */
    <T> JpaUpdate<E> set(String attributeName, T value);

    <T> JpaUpdate<E> set(String attributeName1, T value1, String attributeName2, T value2);

    <T> JpaUpdate<E> set(String attributeName1, T value1, String attributeName2, T value2,
            String attributeName3, T value3);

    /** Set the attribute to the value of another attribute, rather than to a literal value. */
    JpaUpdate<E> setProperty(String attributeName, String anotherAttributeName);

    default <T> JpaUpdate<E> setField(SerializableFunction<E, T> function, Field<T> value) {
        LambdaInfo lambdaInfo = LambdaUtils.inspect(function);
        return setField(lambdaInfo.getAttributeName(), value);
    }

    /** Set the attribute to an expression, e.g. an arithmetic one. */
    <T> JpaUpdate<E> setField(String attributeName, Field<T> value);

    <X> JpaSubQuery<X, X> subQuery(Class<X> entityClass);

    /** Name the table of the subquery, so that it and this statement tell theirs apart. */
    <X> JpaSubQuery<X, X> subQuery(Class<X> entityClass, String alias);

    <X, Y> JpaSubQuery<X, Y> subQuery(Class<X> entityClass, Class<Y> resultClass);

    <X, Y> JpaSubQuery<X, Y> subQuery(Class<X> entityClass, String alias, Class<Y> resultClass);
}
