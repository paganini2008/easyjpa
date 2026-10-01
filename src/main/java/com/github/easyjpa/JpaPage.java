
package com.github.easyjpa;

import com.github.easyjpa.LambdaUtils.LambdaInfo;

/**
 * 
 * A pagination, which counts the total besides listing the rows of one page.
 * 
 * @Description: JpaPage
 * @Author: Fred Feng
 * @Date: 18/10/2024
 * @Version 1.0.0
 */
public interface JpaPage<E, T> {

    JpaPage<E, T> filter(Filter filter);

    JpaPage<E, T> sort(JpaSort... sorts);

    /**
     * Join a subquery as a derived table, whose columns are addressed by the aliases the subquery
     * gave them. The counting query joins it too, so the total stays right.
     */
    JpaPage<E, T> joinSubQuery(SubQueryBuilder<?> subQuery, String alias, Filter on);

    JpaPage<E, T> leftJoinSubQuery(SubQueryBuilder<?> subQuery, String alias, Filter on);

    <X> JpaSubQuery<X, X> subQuery(Class<X> entityClass, String alias);

    <X, Y> JpaSubQuery<X, Y> subQuery(Class<X> entityClass, String alias, Class<Y> resultClass);

    /**
     * Fetch an association along with the entities. The counting query never fetches, and
     * fetching a collection makes the rows be paginated in memory, so prefer it for the to-one
     * associations.
     */
    default <X> JpaPage<E, T> fetch(SerializableFunction<X, ?> function) {
        LambdaInfo info = LambdaUtils.inspect(function);
        return fetch(info.getClassName(), info.getAttributeName());
    }

    default <X> JpaPage<E, T> leftFetch(SerializableFunction<X, ?> function) {
        LambdaInfo info = LambdaUtils.inspect(function);
        return leftFetch(info.getClassName(), info.getAttributeName());
    }

    default JpaPage<E, T> fetch(String attributeName) {
        return fetch(null, attributeName);
    }

    default JpaPage<E, T> leftFetch(String attributeName) {
        return leftFetch(null, attributeName);
    }

    JpaPage<E, T> fetch(String fromAlias, String attributeName);

    JpaPage<E, T> leftFetch(String fromAlias, String attributeName);

    JpaPageResultSet<T> selectThis();

    JpaPageResultSet<T> selectAlias(String... tableAliases);

    JpaPageResultSet<T> select(ColumnList columnList);

    default JpaPageGroupBy<E, T> groupBy(String... attributeNames) {
        return groupBy(new FieldList(attributeNames));
    }

    default JpaPageGroupBy<E, T> groupBy(String alias, String[] attributeNames) {
        return groupBy(new FieldList(alias, attributeNames));
    }

    default JpaPageGroupBy<E, T> groupBy(Field<?>... fields) {
        return groupBy(new FieldList(fields));
    }

    default <X> JpaPageGroupBy<E, T> groupBy(SerializableFunction<X, ?> function) {
        return groupBy(new FieldList(function));
    }

    JpaPageGroupBy<E, T> groupBy(FieldList fieldList);

    /**
     * The lambda tells which entity the attribute belongs to, so the join starts from that entity
     * even if it is not the one the previous join reached, which is how a second branch grows.
     */
    default <X> JpaPage<X, T> join(SerializableFunction<X, ?> function, String alias, Filter on) {
        LambdaInfo info = LambdaUtils.inspect(function);
        return join(info.getClassName(), info.getAttributeName(), alias, on);
    }

    default <X> JpaPage<X, T> leftJoin(SerializableFunction<X, ?> function, String alias, Filter on) {
        LambdaInfo info = LambdaUtils.inspect(function);
        return leftJoin(info.getClassName(), info.getAttributeName(), alias, on);
    }

    default <X> JpaPage<X, T> rightJoin(SerializableFunction<X, ?> function, String alias,
                                        Filter on) {
        LambdaInfo info = LambdaUtils.inspect(function);
        return rightJoin(info.getClassName(), info.getAttributeName(), alias, on);
    }

    <X> JpaPage<X, T> join(Class<X> joinClass, String alias, Filter on);

    <X> JpaPage<X, T> join(String attributeName, String alias, Filter on);

    /** Join from the table behind fromAlias instead of from the entity the last join reached. */
    <X> JpaPage<X, T> join(String fromAlias, String attributeName, String alias, Filter on);

    <X> JpaPage<X, T> leftJoin(Class<X> joinClass, String alias, Filter on);

    <X> JpaPage<X, T> leftJoin(String attributeName, String alias, Filter on);

    <X> JpaPage<X, T> leftJoin(String fromAlias, String attributeName, String alias, Filter on);

    <X> JpaPage<X, T> rightJoin(Class<X> joinClass, String alias, Filter on);

    <X> JpaPage<X, T> rightJoin(String attributeName, String alias, Filter on);

    <X> JpaPage<X, T> rightJoin(String fromAlias, String attributeName, String alias, Filter on);

    <X> JpaPage<X, T> crossJoin(Class<X> joinClass, String alias);

}
