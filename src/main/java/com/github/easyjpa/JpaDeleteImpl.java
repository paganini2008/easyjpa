
package com.github.easyjpa;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * 
 * Default JpaDelete implementation.
 * 
 * @Description: JpaDeleteImpl
 * @Author: Fred Feng
 * @Date: 18/10/2024
 * @Version 1.0.0
 */
public class JpaDeleteImpl<E> implements JpaDelete<E> {

    private final Model<E> model;
    private final CriteriaDelete<E> delete;
    private final CriteriaBuilder builder;
    private final JpaCustomUpdate<E> customUpdate;

    JpaDeleteImpl(Model<E> model, CriteriaDelete<E> delete, CriteriaBuilder builder,
            JpaCustomUpdate<E> customUpdate) {
        this.model = model;
        this.delete = delete;
        this.builder = builder;
        this.customUpdate = customUpdate;
    }

    @Override
    public JpaDelete<E> filter(Filter filter) {
        if (filter != null) {
            delete.where(filter.toPredicate(model, builder));
        }
        return this;
    }

    @Override
    public <X> JpaSubQuery<X, X> subQuery(Class<X> entityClass) {
        return subQuery(entityClass, defaultAlias(entityClass));
    }

    @Override
    public <X> JpaSubQuery<X, X> subQuery(Class<X> entityClass, String alias) {
        Subquery<X> subquery = delete.subquery(entityClass);
        Root<X> root = subquery.from(entityClass);
        return new JpaSubQueryImpl<X, X>(sibling(root, alias), subquery, builder);
    }

    @Override
    public <X, Y> JpaSubQuery<X, Y> subQuery(Class<X> entityClass, Class<Y> resultClass) {
        return subQuery(entityClass, defaultAlias(entityClass), resultClass);
    }

    @Override
    public <X, Y> JpaSubQuery<X, Y> subQuery(Class<X> entityClass, String alias,
            Class<Y> resultClass) {
        Subquery<Y> subquery = delete.subquery(resultClass);
        Root<X> root = subquery.from(entityClass);
        return new JpaSubQueryImpl<X, Y>(sibling(root, alias), subquery, builder);
    }

    /**
     * The subquery keeps the model of this statement behind its own, so that the attributes of the
     * entity being deleted stay reachable, which is how the two get correlated.
     */
    private <X> Model<X> sibling(Root<X> root, String alias) {
        return model.sibling(new RootModel<X>(root, alias, model.getMetamodel()));
    }

    /** An alias of its own, since the one of this statement is already taken. */
    private String defaultAlias(Class<?> entityClass) {
        return entityClass.getSimpleName().toLowerCase();
    }

    @Override
    public int execute() {
        return customUpdate.executeUpdate((CriteriaBuilder builder) -> delete);
    }

}
