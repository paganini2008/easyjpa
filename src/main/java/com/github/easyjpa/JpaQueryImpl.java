package com.github.easyjpa;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.criteria.Subquery;

/**
 * 
 * Default JpaQuery implementation.
 * 
 * @Description: JpaQueryImpl
 * @Author: Fred Feng
 * @Date: 07/10/2024
 * @Version 1.0.0
 */
public class JpaQueryImpl<E, T> implements JpaQuery<E, T> {

    JpaQueryImpl(Model<E> model, CriteriaQuery<T> query, CriteriaBuilder builder,
            JpaCustomQuery<?> customQuery) {
        this.model = model;
        this.query = query;
        this.builder = builder;
        this.customQuery = customQuery;
    }

    private final Model<E> model;
    private final CriteriaQuery<T> query;
    private final CriteriaBuilder builder;
    private final JpaCustomQuery<?> customQuery;

    @Override
    public JpaQuery<E, T> filter(Filter filter) {
        if (filter != null) {
            query.where(filter.toPredicate(model, builder));
        }
        return this;
    }

    @Override
    public JpaGroupBy<E, T> groupBy(FieldList fieldList) {
        List<Expression<?>> expressions = new ArrayList<Expression<?>>();
        for (Field<?> field : fieldList) {
            expressions.add(field.toExpression(model, builder));
        }
        query.groupBy(expressions);
        return new JpaGroupByImpl<E, T>(model, query, builder, customQuery);
    }

    @Override
    public <X> JpaSubQuery<X, X> subQuery(Class<X> entityClass, String alias) {
        Subquery<X> subquery = query.subquery(entityClass);
        Root<X> root = subquery.from(entityClass);
        Model<X> model =
                this.model.sibling(new RootModel<X>(root, alias, this.model.getMetamodel()));
        return new JpaSubQueryImpl<X, X>(model, subquery.select(root), builder);
    }

    @Override
    public <X, Y> JpaSubQuery<X, Y> subQuery(Class<X> entityClass, String alias,
            Class<Y> resultClass) {
        Subquery<Y> subquery = query.subquery(resultClass);
        Root<X> root = subquery.from(entityClass);
        Model<X> model =
                this.model.sibling(new RootModel<X>(root, alias, this.model.getMetamodel()));
        return new JpaSubQueryImpl<X, Y>(model, subquery, builder);
    }

    @Override
    public JpaQuery<E, T> joinSubQuery(SubQueryBuilder<?> subQuery, String alias, Filter on) {
        return joinSubQuery(subQuery, alias, on, JoinType.INNER);
    }

    @Override
    public JpaQuery<E, T> leftJoinSubQuery(SubQueryBuilder<?> subQuery, String alias, Filter on) {
        return joinSubQuery(subQuery, alias, on, JoinType.LEFT);
    }

    private JpaQuery<E, T> joinSubQuery(SubQueryBuilder<?> subQuery, String alias, Filter on,
            JoinType joinType) {
        Model<E> joined = JpaProviders.getProvider().joinSubQuery(model,
                subQuery.toSubquery(builder), alias, joinType, on, builder);
        return new JpaQueryImpl<E, T>(joined, query, builder, customQuery);
    }

    @Override
    public JpaQuery<E, T> fetch(String fromAlias, String attributeName) {
        model.fetch(fromAlias, attributeName, JoinType.INNER);
        return this;
    }

    @Override
    public JpaQuery<E, T> leftFetch(String fromAlias, String attributeName) {
        model.fetch(fromAlias, attributeName, JoinType.LEFT);
        return this;
    }

    @Override
    public JpaQueryResultSet<T> selectThis() {
        if (query.getResultType() == model.getRootType()) {
            return selectAlias(new String[0]);
        }
        return selectAlias(Model.ROOT);
    }

    @Override
    public JpaQueryResultSet<T> selectAlias(String... tableAliases) {
        if (ArrayUtils.isNotEmpty(tableAliases)) {
            List<Selection<?>> selections = new ArrayList<Selection<?>>();
            for (String tableAlias : tableAliases) {
                selections.add(model.getSelection(tableAlias));
            }
            select(selections);
        }
        return new JpaQueryResultSetImpl<T>(model, query, customQuery);
    }

    @Override
    public JpaQueryResultSet<T> select(ColumnList columnList) {
        if (columnList != null) {
            List<Selection<?>> selections = new ArrayList<Selection<?>>();
            for (Column column : columnList) {
                selections.add(column.toSelection(model, builder));
            }
            select(selections);
        }
        return new JpaQueryResultSetImpl<T>(model, query, customQuery);
    }

    @Override
    public T one(Column column) {
        select(List.of(column.toSelection(model, builder)));
        return customQuery.getSingleResult(builder -> query);
    }

    // A multiselect of one selection would make Hibernate instantiate the result type by that
    // value, which fails for the types having no such constructor, so select the only selection
    // directly once it is of the result type itself.
    @SuppressWarnings("unchecked")
    private void select(List<Selection<?>> selections) {
        if (selections.size() == 1) {
            Selection<?> selection = selections.get(0);
            Class<T> resultType = query.getResultType();
            if (resultType != Tuple.class && resultType != Object[].class
                    && resultType.isAssignableFrom(selection.getJavaType())) {
                query.select((Selection<? extends T>) selection);
                return;
            }
        }
        query.multiselect(selections);
    }

    @Override
    public JpaQuery<E, T> sort(JpaSort... sorts) {
        if (ArrayUtils.isNotEmpty(sorts)) {
            List<Order> orders = new ArrayList<Order>();
            for (JpaSort sort : sorts) {
                orders.add(sort.toOrder(model, builder));
            }
            query.orderBy(orders);
        }
        return this;
    }

    @Override
    public JpaQuery<E, T> distinct() {
        query.distinct(true);
        return this;
    }

    @Override
    public <X> JpaQuery<X, T> crossJoin(Class<X> joinClass, String alias) {
        Root<X> root = query.from(joinClass);
        RootModel<X> sibling = new RootModel<X>(root, alias, model.getMetamodel());
        SiblingModel<E, X> siblingModel = new SiblingModel<>(sibling, model);
        return new JpaQueryImpl<X, T>(siblingModel, query, builder, customQuery);
    }

    @Override
    public <X> JpaQuery<X, T> join(Class<X> joinClass, String alias, Filter on) {
        Model<X> join = on(model.join(joinClass, alias, null), on);
        return new JpaQueryImpl<X, T>(join, query, builder, customQuery);
    }

    @Override
    public <X> JpaQuery<X, T> join(String attributeName, String alias, Filter on) {
        Model<X> join = on(model.join(attributeName, alias, null), on);
        return new JpaQueryImpl<X, T>(join, query, builder, customQuery);
    }

    @Override
    public <X> JpaQuery<X, T> join(String fromAlias, String attributeName, String alias, Filter on) {
        Model<X> join = on(model.join(fromAlias, attributeName, alias, null), on);
        return new JpaQueryImpl<X, T>(join, query, builder, customQuery);
    }

    @Override
    public <X> JpaQuery<X, T> leftJoin(Class<X> joinClass, String alias, Filter on) {
        Model<X> join = on(model.leftJoin(joinClass, alias, null), on);
        return new JpaQueryImpl<X, T>(join, query, builder, customQuery);
    }

    @Override
    public <X> JpaQuery<X, T> leftJoin(String attributeName, String alias, Filter on) {
        Model<X> join = on(model.leftJoin(attributeName, alias, null), on);
        return new JpaQueryImpl<X, T>(join, query, builder, customQuery);
    }

    @Override
    public <X> JpaQuery<X, T> leftJoin(String fromAlias, String attributeName, String alias, Filter on) {
        Model<X> join = on(model.leftJoin(fromAlias, attributeName, alias, null), on);
        return new JpaQueryImpl<X, T>(join, query, builder, customQuery);
    }

    @Override
    public <X> JpaQuery<X, T> rightJoin(Class<X> joinClass, String alias, Filter on) {
        Model<X> join = on(model.rightJoin(joinClass, alias, null), on);
        return new JpaQueryImpl<X, T>(join, query, builder, customQuery);
    }

    @Override
    public <X> JpaQuery<X, T> rightJoin(String attributeName, String alias, Filter on) {
        Model<X> join = on(model.rightJoin(attributeName, alias, null), on);
        return new JpaQueryImpl<X, T>(join, query, builder, customQuery);
    }

    @Override
    public <X> JpaQuery<X, T> rightJoin(String fromAlias, String attributeName, String alias, Filter on) {
        Model<X> join = on(model.rightJoin(fromAlias, attributeName, alias, null), on);
        return new JpaQueryImpl<X, T>(join, query, builder, customQuery);
    }

    // The on condition is applied after the join is established, so that it may refer to the
    // attributes of the joined entity as well.
    private <X> Model<X> on(Model<X> join, Filter on) {
        if (on != null && join instanceof JoinModel) {
            ((JoinModel<?, X>) join).on(on.toPredicate(join, builder));
        }
        return join;
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
