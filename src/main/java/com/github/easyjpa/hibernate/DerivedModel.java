
package com.github.easyjpa.hibernate;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.query.criteria.JpaDerivedJoin;
import org.hibernate.query.criteria.JpaFrom;
import org.hibernate.query.sqm.tree.SqmJoinType;
import com.github.easyjpa.Filter;
import com.github.easyjpa.JpaAttributeDetail;
import com.github.easyjpa.Model;
import com.github.easyjpa.SiblingModel;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;

/**
 *
 * A derived table, that is to say, a subquery occurring in the from clause, which is joined like a
 * table and whose columns are addressed by the aliases the subquery gave them.
 *
 * <p>
 * It carries no entity, so it neither joins an association nor gets selected as a whole. Everything
 * else falls through to the model it is joined to.
 * </p>
 *
 * @Description: DerivedModel
 * @Author: Fred Feng
 * @Date: 17/08/2026
 * @Version 1.0.0
 */
public class DerivedModel<X, Y> implements Model<Y> {

    private final JpaDerivedJoin<Y> derivedJoin;
    private final String alias;
    private final Model<X> model;

    DerivedModel(JpaDerivedJoin<Y> derivedJoin, String alias, Model<X> model) {
        this.derivedJoin = derivedJoin;
        this.alias = alias;
        this.model = model;
    }

    /**
     * Join the subquery as a derived table, keeping the given model the one in focus. The on
     * condition is evaluated afterwards, so that it can refer to the columns of that table.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static <X, Y> Model<X> join(Model<X> model, Subquery<Y> subquery, String alias,
            JoinType joinType, Filter on, CriteriaBuilder builder) {
        JpaDerivedJoin<Y> derivedJoin =
                ((JpaFrom<?, ?>) model.getFrom()).join(subquery, toSqmJoinType(joinType));
        derivedJoin.alias(alias);
        DerivedModel<X, Y> derivedModel = new DerivedModel(derivedJoin, alias, model);
        Model<X> joined = new SiblingModel(model, derivedModel);
        if (on != null) {
            derivedModel.on(on.toPredicate(joined, builder));
        }
        return joined;
    }

    private static SqmJoinType toSqmJoinType(JoinType joinType) {
        switch (joinType) {
            case LEFT:
                return SqmJoinType.LEFT;
            case RIGHT:
                return SqmJoinType.RIGHT;
            default:
                return SqmJoinType.INNER;
        }
    }

    /** Add the on condition after the derived table is joined, so that it can refer to it. */
    void on(Predicate predicate) {
        derivedJoin.on(predicate);
    }

    @Override
    public EntityType<Y> getEntityType() {
        throw new UnsupportedOperationException("A derived table carries no entity: " + alias);
    }

    @Override
    public Class<?> getRootType() {
        return model.getRootType();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<Y> getType() {
        return (Class<Y>) derivedJoin.getJavaType();
    }

    @Override
    public boolean isManaged(Class<?> type) {
        return model.isManaged(type);
    }

    @Override
    public Root<?> getRoot() {
        return model.getRoot();
    }

    @Override
    public Metamodel getMetamodel() {
        return model.getMetamodel();
    }

    @Override
    public String getAlias() {
        return alias;
    }

    @Override
    public From<?, ?> getFrom() {
        return derivedJoin;
    }

    @Override
    public String aliasOf(String className) {
        return model.aliasOf(className);
    }

    @Override
    public From<?, ?> getFrom(String alias) {
        return this.alias.equals(alias) ? derivedJoin : model.getFrom(alias);
    }

    @Override
    public boolean hasAttribute(String name, String attributeName) {
        return this.alias.equals(name) ? true : model.hasAttribute(name, attributeName);
    }

    @Override
    public <T> Path<T> getAttribute(String attributeName) {
        return derivedJoin.get(attributeName);
    }

    @Override
    public <T> Path<T> getAttribute(String name, String attributeName) {
        return this.alias.equals(name) ? getAttribute(attributeName)
                : model.getAttribute(name, attributeName);
    }

    @Override
    public Selection<?> getSelection(String alias) {
        if (this.alias.equals(alias)) {
            throw new UnsupportedOperationException(
                    "A derived table is selected column by column: " + alias);
        }
        return model.getSelection(alias);
    }

    @Override
    public List<Selection<?>> getSelections(String alias, String[] attributeNames) {
        if (!this.alias.equals(alias)) {
            return model.getSelections(alias, attributeNames);
        }
        List<Selection<?>> selections = new ArrayList<Selection<?>>();
        for (String attributeName : attributeNames) {
            selections.add(getAttribute(attributeName).alias(attributeName));
        }
        return selections;
    }

    @Override
    public List<JpaAttributeDetail> getAttributeDetails(String alias) {
        return this.alias.equals(alias) ? new ArrayList<JpaAttributeDetail>()
                : model.getAttributeDetails(alias);
    }

    @Override
    public boolean isAssociatedAttribute(String attribute, Class<?> clz) {
        return false;
    }

    // A derived table has no association to join, so the joins grow on the model behind it

    @Override
    public <Z> Model<Z> join(Class<Z> joinClass, String alias, Predicate on) {
        return model.join(joinClass, alias, on);
    }

    @Override
    public <Z> Model<Z> join(String attributeName, String alias, Predicate on) {
        return model.join(attributeName, alias, on);
    }

    @Override
    public <Z> Model<Z> leftJoin(Class<Z> joinClass, String alias, Predicate on) {
        return model.leftJoin(joinClass, alias, on);
    }

    @Override
    public <Z> Model<Z> leftJoin(String attributeName, String alias, Predicate on) {
        return model.leftJoin(attributeName, alias, on);
    }

    @Override
    public <Z> Model<Z> rightJoin(Class<Z> joinClass, String alias, Predicate on) {
        return model.rightJoin(joinClass, alias, on);
    }

    @Override
    public <Z> Model<Z> rightJoin(String attributeName, String alias, Predicate on) {
        return model.rightJoin(attributeName, alias, on);
    }

    @Override
    public <S> Model<S> sibling(Model<S> sibling) {
        return new SiblingModel<Y, S>(sibling, this);
    }

}
