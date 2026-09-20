package com.github.easyjpa;

import com.github.easyjpa.LambdaUtils.LambdaInfo;

/**
 * 
 * A subquery, which can be a filter condition or a select item of the outer query.
 * 
 * @Description: JpaSubQuery
 * @Author: Fred Feng
 * @Date: 18/10/2024
 * @Version 1.0.0
 */
public interface JpaSubQuery<X, Y> extends SubQueryBuilder<Y> {

    JpaSubQuery<X, Y> filter(Filter filter);

    default JpaSubQueryGroupBy<X, Y> groupBy(String alias, String... attributeNames) {
        return groupBy(new FieldList(alias, attributeNames));
    }

    default JpaSubQueryGroupBy<X, Y> groupBy(Field<?>... fields) {
        return groupBy(new FieldList(fields));
    }

    JpaSubQueryGroupBy<X, Y> groupBy(FieldList fieldList);

    JpaSubQuery<X, Y> select(String alias, String attributeName);

    default JpaSubQuery<X, Y> select(SerializableFunction<X, Y> function) {
        return select(Property.forName(function));
    }

    JpaSubQuery<X, Y> select(Field<Y> field);

    /**
     * Select several columns, which is what a derived table needs. Every column has to be aliased,
     * since the aliases become the column names of that table.
     */
    JpaSubQuery<X, Y> select(ColumnList columnList);

    /** Distinct the selection, so it has to be called after the selection is given. */
    JpaSubQuery<X, Y> distinct();

    <Z> JpaSubQuery<Z, Y> join(String attributeName, String alias, Filter on);

    <Z> JpaSubQuery<Z, Y> join(String fromAlias, String attributeName, String alias, Filter on);

    <Z> JpaSubQuery<Z, Y> leftJoin(String attributeName, String alias, Filter on);

    <Z> JpaSubQuery<Z, Y> leftJoin(String fromAlias, String attributeName, String alias, Filter on);

    <Z> JpaSubQuery<Z, Y> rightJoin(String attributeName, String alias, Filter on);

    <Z> JpaSubQuery<Z, Y> rightJoin(String fromAlias, String attributeName, String alias, Filter on);

    /**
     * The lambda tells which entity the attribute belongs to, so the join starts from that entity
     * even if it is not the one the previous join reached, which is how a second branch grows.
     */
    default <Z> JpaSubQuery<Z, Y> join(SerializableFunction<Z, ?> function, String alias,
            Filter on) {
        LambdaInfo info = LambdaUtils.inspect(function);
        return join(info.getClassName(), info.getAttributeName(), alias, on);
    }

    default <Z> JpaSubQuery<Z, Y> leftJoin(SerializableFunction<Z, ?> function, String alias,
                                           Filter on) {
        LambdaInfo info = LambdaUtils.inspect(function);
        return leftJoin(info.getClassName(), info.getAttributeName(), alias, on);
    }

    default <Z> JpaSubQuery<Z, Y> rightJoin(SerializableFunction<Z, ?> function, String alias,
                                            Filter on) {
        LambdaInfo info = LambdaUtils.inspect(function);
        return rightJoin(info.getClassName(), info.getAttributeName(), alias, on);
    }

}
