
package com.github.easyjpa;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 *
 * Everything a counting query does apart from counting: it keeps the same filtering, joining and
 * grouping clauses as the pagination it belongs to. How the rows or the groups are finally counted
 * is left to the provider.
 *
 * @Description: AbstractPageCount
 * @Author: Fred Feng
 * @Date: 17/08/2026
 * @Version 1.0.0
 */
public abstract class AbstractPageCount<E> implements JpaPageCount<E> {

    protected final Model<E> model;
    protected final AbstractQuery<Long> countQuery;
    protected final CriteriaBuilder builder;

    protected AbstractPageCount(Model<E> model, AbstractQuery<Long> countQuery,
            CriteriaBuilder builder) {
        this.model = model;
        this.countQuery = countQuery;
        this.builder = builder;
    }

    /** Carry the same counting query over to the model a join has just reached. */
    protected abstract <X> JpaPageCount<X> sibling(Model<X> model);

    @Override
    public JpaPageCount<E> filter(Filter filter) {
        if (filter != null) {
            countQuery.where(filter.toPredicate(model, builder));
        }
        return this;
    }

    @Override
    public JpaPageCount<E> groupBy(FieldList fieldList) {
        if (fieldList != null) {
            List<Expression<?>> expressions = new ArrayList<Expression<?>>();
            for (Field<?> field : fieldList) {
                expressions.add(field.toExpression(model, builder));
            }
            countQuery.groupBy(expressions);
        }
        return this;
    }

    @Override
    public JpaPageCount<E> having(Filter filter) {
        if (filter != null) {
            countQuery.having(filter.toPredicate(model, builder));
        }
        return this;
    }

    @Override
    public <X> JpaPageCount<X> join(Class<X> joinClass, String alias, Filter on) {
        return sibling(on(model.join(joinClass, alias, null), on));
    }

    @Override
    public <X> JpaPageCount<X> join(String attributeName, String alias, Filter on) {
        return sibling(on(model.join(attributeName, alias, null), on));
    }

    @Override
    public <X> JpaPageCount<X> join(String fromAlias, String attributeName, String alias,
            Filter on) {
        return sibling(on(model.join(fromAlias, attributeName, alias, null), on));
    }

    @Override
    public <X> JpaPageCount<X> leftJoin(Class<X> joinClass, String alias, Filter on) {
        return sibling(on(model.leftJoin(joinClass, alias, null), on));
    }

    @Override
    public <X> JpaPageCount<X> leftJoin(String attributeName, String alias, Filter on) {
        return sibling(on(model.leftJoin(attributeName, alias, null), on));
    }

    @Override
    public <X> JpaPageCount<X> leftJoin(String fromAlias, String attributeName, String alias,
            Filter on) {
        return sibling(on(model.leftJoin(fromAlias, attributeName, alias, null), on));
    }

    @Override
    public <X> JpaPageCount<X> rightJoin(Class<X> joinClass, String alias, Filter on) {
        return sibling(on(model.rightJoin(joinClass, alias, null), on));
    }

    @Override
    public <X> JpaPageCount<X> rightJoin(String attributeName, String alias, Filter on) {
        return sibling(on(model.rightJoin(attributeName, alias, null), on));
    }

    @Override
    public <X> JpaPageCount<X> rightJoin(String fromAlias, String attributeName, String alias,
            Filter on) {
        return sibling(on(model.rightJoin(fromAlias, attributeName, alias, null), on));
    }

    @Override
    public <X> JpaPageCount<X> crossJoin(Class<X> joinClass, String alias) {
        Root<X> root = countQuery.from(joinClass);
        RootModel<X> sibling = new RootModel<X>(root, alias, model.getMetamodel());
        return sibling(new SiblingModel<E, X>(sibling, model));
    }

    private <X> Model<X> on(Model<X> join, Filter on) {
        if (on != null && join instanceof JoinModel) {
            ((JoinModel<?, X>) join).on(on.toPredicate(join, builder));
        }
        return join;
    }

    @Override
    public JpaPageCount<E> joinSubQuery(SubQueryBuilder<?> subQuery, String alias, Filter on) {
        return joinSubQuery(subQuery, alias, on, JoinType.INNER);
    }

    @Override
    public JpaPageCount<E> leftJoinSubQuery(SubQueryBuilder<?> subQuery, String alias, Filter on) {
        return joinSubQuery(subQuery, alias, on, JoinType.LEFT);
    }

    private JpaPageCount<E> joinSubQuery(SubQueryBuilder<?> subQuery, String alias, Filter on,
            JoinType joinType) {
        return sibling(JpaProviders.getProvider().joinSubQuery(model,
                subQuery.toSubquery(builder), alias, joinType, on, builder));
    }

    @Override
    public Model<E> model() {
        return model;
    }


}
