package com.github.easyjpa;

import com.github.easyjpa.LambdaUtils.LambdaInfo;
import jakarta.persistence.criteria.CriteriaQuery;

/**
 * 
 * A query statement, which joins, filters, groups, sorts and selects.
 * 
 * @Description: JpaQuery
 * @Author: Fred Feng
 * @Date: 07/10/2024
 * @Version 1.0.0
 */
public interface JpaQuery<E, T> {

    JpaQuery<E, T> filter(Filter filter);

    JpaQuery<E, T> sort(JpaSort... sorts);

    default JpaGroupBy<E, T> groupBy(String... attributeNames) {
        return groupBy(new FieldList(attributeNames));
    }

    default JpaGroupBy<E, T> groupBy(String alias, String[] attributeNames) {
        return groupBy(new FieldList(alias, attributeNames));
    }

    default JpaGroupBy<E, T> groupBy(Field<?>... fields) {
        return groupBy(new FieldList(fields));
    }

    default <X> JpaGroupBy<E, T> groupBy(SerializableFunction<X, ?> function) {
        return groupBy(new FieldList(function));
    }

    JpaGroupBy<E, T> groupBy(FieldList fieldList);

    /**
     * Fetch an association along with the entities, so that reading it afterwards runs no extra
     * statement. JPA requires the entity owning the association to be the one selected, and
     * fetching a collection together with a pagination makes the rows be paginated in memory.
     */
    default <X> JpaQuery<E, T> fetch(SerializableFunction<X, ?> function) {
        LambdaInfo info = LambdaUtils.inspect(function);
        return fetch(info.getClassName(), info.getAttributeName());
    }

    default <X> JpaQuery<E, T> leftFetch(SerializableFunction<X, ?> function) {
        LambdaInfo info = LambdaUtils.inspect(function);
        return leftFetch(info.getClassName(), info.getAttributeName());
    }

    default JpaQuery<E, T> fetch(String attributeName) {
        return fetch(null, attributeName);
    }

    default JpaQuery<E, T> leftFetch(String attributeName) {
        return leftFetch(null, attributeName);
    }

    JpaQuery<E, T> fetch(String fromAlias, String attributeName);

    JpaQuery<E, T> leftFetch(String fromAlias, String attributeName);

    /** Select the root entity itself. */
    JpaQueryResultSet<T> selectThis();

    /** Select the entities behind the given table aliases, one column per alias. */
    JpaQueryResultSet<T> selectAlias(String... tableAliases);

    JpaQueryResultSet<T> select(ColumnList columnList);

    default T one(SerializableFunction<E, T> function) {
        return one(Property.forName(function));
    }

    default T one(String attributeName) {
        return one(Column.forName(attributeName));
    }

    default T one(String alias, String attributeName) {
        return one(Column.forName(alias, attributeName));
    }

    default T one(Field<T> field) {
        return one(field.as(field.toString()));
    }

    /** Select a single column and return the only value of it. */
    T one(Column column);

    JpaQuery<E, T> distinct();

    /**
     * Join a subquery as a derived table, whose columns are addressed by the aliases the subquery
     * gave them. The entity in focus stays the same, so the following clauses keep working on it.
     */
    JpaQuery<E, T> joinSubQuery(SubQueryBuilder<?> subQuery, String alias, Filter on);

    JpaQuery<E, T> leftJoinSubQuery(SubQueryBuilder<?> subQuery, String alias, Filter on);

    /** The subquery belongs to this statement, so it can only be used by this statement. */
    <X> JpaSubQuery<X, X> subQuery(Class<X> entityClass, String alias);

    <X, Y> JpaSubQuery<X, Y> subQuery(Class<X> entityClass, String alias, Class<Y> resultClass);

    <X> JpaQuery<X, T> join(Class<X> joinClass, String alias, Filter on);

    <X> JpaQuery<X, T> join(String attributeName, String alias, Filter on);

    /** Join from the table behind fromAlias instead of from the entity the last join reached. */
    <X> JpaQuery<X, T> join(String fromAlias, String attributeName, String alias, Filter on);

    <X> JpaQuery<X, T> leftJoin(Class<X> joinClass, String alias, Filter on);

    <X> JpaQuery<X, T> leftJoin(String attributeName, String alias, Filter on);

    <X> JpaQuery<X, T> leftJoin(String fromAlias, String attributeName, String alias, Filter on);

    <X> JpaQuery<X, T> rightJoin(Class<X> joinClass, String alias, Filter on);

    <X> JpaQuery<X, T> rightJoin(String attributeName, String alias, Filter on);

    <X> JpaQuery<X, T> rightJoin(String fromAlias, String attributeName, String alias, Filter on);

    <X> JpaQuery<X, T> crossJoin(Class<X> joinClass, String alias);

    /**
     * The lambda tells which entity the attribute belongs to, so the join starts from that entity
     * even if it is not the one the previous join reached, which is how a second branch grows.
     */
    default <X> JpaQuery<X, T> join(SerializableFunction<X, ?> function, String alias, Filter on) {
        LambdaInfo info = LambdaUtils.inspect(function);
        return join(info.getClassName(), info.getAttributeName(), alias, on);
    }

    default <X> JpaQuery<X, T> leftJoin(SerializableFunction<X, ?> function, String alias,
                                        Filter on) {
        LambdaInfo info = LambdaUtils.inspect(function);
        return leftJoin(info.getClassName(), info.getAttributeName(), alias, on);
    }

    default <X> JpaQuery<X, T> rightJoin(SerializableFunction<X, ?> function, String alias,
                                         Filter on) {
        LambdaInfo info = LambdaUtils.inspect(function);
        return rightJoin(info.getClassName(), info.getAttributeName(), alias, on);
    }

    CriteriaQuery<T> query();

    Model<E> model();

}
