
package com.github.easyjpa;

/**
 *
 * The counting query of a pagination, which keeps the same filtering, joining and grouping clauses
 * as the pagination itself. How the total gets counted is left to the provider, see
 * {@link JpaProvider#createPageCount(Class, String, jakarta.persistence.EntityManager)}.
 *
 * @Description: JpaPageCount
 * @Author: Fred Feng
 * @Date: 17/08/2026
 * @Version 1.0.0
 */
public interface JpaPageCount<E> {

    JpaPageCount<E> filter(Filter filter);

    JpaPageCount<E> groupBy(FieldList fieldList);

    JpaPageCount<E> having(Filter filter);

    <X> JpaPageCount<X> join(Class<X> joinClass, String alias, Filter on);

    <X> JpaPageCount<X> join(String attributeName, String alias, Filter on);

    <X> JpaPageCount<X> join(String fromAlias, String attributeName, String alias, Filter on);

    <X> JpaPageCount<X> leftJoin(Class<X> joinClass, String alias, Filter on);

    <X> JpaPageCount<X> leftJoin(String attributeName, String alias, Filter on);

    <X> JpaPageCount<X> leftJoin(String fromAlias, String attributeName, String alias, Filter on);

    <X> JpaPageCount<X> rightJoin(Class<X> joinClass, String alias, Filter on);

    <X> JpaPageCount<X> rightJoin(String attributeName, String alias, Filter on);

    <X> JpaPageCount<X> rightJoin(String fromAlias, String attributeName, String alias, Filter on);

    <X> JpaPageCount<X> crossJoin(Class<X> joinClass, String alias);

    JpaPageCount<E> joinSubQuery(SubQueryBuilder<?> subQuery, String alias, Filter on);

    JpaPageCount<E> leftJoinSubQuery(SubQueryBuilder<?> subQuery, String alias, Filter on);

    /**
     * Count the total rows, or the total groups if a grouping clause is present.
     *
     * @param customQuery the query executor
     * @return the total rows
     */
    long rowCount(JpaCustomQuery<?> customQuery);

    Model<E> model();

}
