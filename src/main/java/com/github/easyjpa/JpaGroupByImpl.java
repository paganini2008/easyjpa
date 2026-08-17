
package com.github.easyjpa;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Selection;

/**
 * 
 * Default JpaGroupBy implementation.
 * 
 * @Description: JpaGroupByImpl
 * @Author: Fred Feng
 * @Date: 18/10/2024
 * @Version 1.0.0
 */
public class JpaGroupByImpl<E, T> implements JpaGroupBy<E, T> {

    private final Model<E> model;
    private final CriteriaQuery<T> query;
    private final CriteriaBuilder builder;
    private final JpaCustomQuery<?> customQuery;

    JpaGroupByImpl(Model<E> model, CriteriaQuery<T> query, CriteriaBuilder builder,
            JpaCustomQuery<?> customQuery) {
        this.model = model;
        this.query = query;
        this.builder = builder;
        this.customQuery = customQuery;
    }

    public JpaGroupBy<E, T> having(Filter filter) {
        query.having(filter.toPredicate(model, builder));
        return this;
    }

    public JpaQueryResultSet<T> select(ColumnList columnList) {
        if (columnList != null) {
            List<Selection<?>> selections = new ArrayList<Selection<?>>();
            for (Column column : columnList) {
                selections.add(column.toSelection(model, builder));
            }
            query.multiselect(selections);
        }
        return new JpaQueryResultSetImpl<T>(model, query, customQuery);
    }

    public JpaGroupBy<E, T> sort(JpaSort... sorts) {
        List<Order> orders = new ArrayList<Order>();
        for (JpaSort sort : sorts) {
            orders.add(sort.toOrder(model, builder));
        }
        if (!orders.isEmpty()) {
            query.orderBy(orders);
        }
        return this;
    }

    @Override
    public CriteriaQuery<T> query() {
        return query;
    }

    @Override
    public Model<E> model() {
        return model;
    }

}
