
package com.github.easyjpa;

import jakarta.persistence.criteria.CriteriaQuery;

/**
 *
 * Default JpaPageGroupBy implementation.
 * 
 * @Description: JpaPageGroupByImpl
 * @Author: Fred Feng
 * @Date: 18/10/2024
 * @Version 1.0.0
 */
public class JpaPageGroupByImpl<E, T> implements JpaPageGroupBy<E, T> {

    private final JpaGroupBy<E, T> query;
    private final JpaPageCount<E> counter;
    private final JpaCustomQuery<?> customQuery;

    JpaPageGroupByImpl(JpaGroupBy<E, T> query, JpaPageCount<E> counter,
            JpaCustomQuery<?> customQuery) {
        this.query = query;
        this.counter = counter;
        this.customQuery = customQuery;
    }

    @Override
    public JpaPageGroupBy<E, T> having(Filter expression) {
        query.having(expression);
        counter.having(expression);
        return this;

    }

    @Override
    public JpaPageResultSet<T> select(ColumnList columnList) {
        query.select(columnList);
        return new JpaGroupPageResultSetImpl<T>(query.model(), query.query(), counter, customQuery);
    }

    @Override
    public JpaPageGroupBy<E, T> sort(JpaSort... sorts) {
        query.sort(sorts);
        return this;
    }

    @Override
    public CriteriaQuery<T> query() {
        return query.query();
    }

    @Override
    public Model<E> model() {
        return query.model();
    }

}
