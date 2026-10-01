package com.github.easyjpa;

import java.util.List;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;

/**
 * 
 * Model represents a wrapped Object of Root, which is a from clause
 * 
 * @Author: Fred Feng
 * @Date: 07/10/2024
 * @Version 1.0.0
 */
public interface Model<X> {

    static final String ROOT = "this";

    EntityType<X> getEntityType();

    Class<?> getRootType();

    Class<X> getType();

    boolean isManaged(Class<?> type);

    Root<?> getRoot();

    Metamodel getMetamodel();

    String getAlias();

    <T> Path<T> getAttribute(String attributeName);

    <T> Path<T> getAttribute(String name, String attributeName);

    boolean hasAttribute(String name, String attributeName);

    Selection<?> getSelection(String alias);

    List<Selection<?>> getSelections(String alias, String[] attributeNames);

    default List<JpaAttributeDetail> getAttributeDetails() {
        return getAttributeDetails(ROOT);
    }

    List<JpaAttributeDetail> getAttributeDetails(String alias);

    boolean isAssociatedAttribute(String attribute, Class<?> clz);

    /** The from clause of this model, which is the root itself or the last join it reached. */
    From<?, ?> getFrom();

    /** The from clause behind the alias, or null if no table is aliased so. */
    From<?, ?> getFrom(String alias);

    /**
     * The alias given to the nearest table of that entity, or null if this statement joins none.
     * This is what resolves a lambda, which only carries the entity it belongs to, and it resolves
     * it against this statement alone, so the subqueries never see the aliases of the outer one.
     */
    String aliasOf(String className);

    /**
     * Fetch an association along with the entity, which spares the extra statement the lazy
     * loading would run afterwards. It only pays off when the entities themselves are selected.
     */
    default Model<X> fetch(String fromAlias, String attributeName, JoinType joinType) {
        From<?, ?> from = lookupFrom(fromAlias);
        (from != null ? from : getFrom()).fetch(attributeName, joinType);
        return this;
    }

    /**
     * Join from the table behind the given alias rather than from this model, which is how a
     * second branch starts from a table joined before. It falls back to this model once the alias
     * is unknown.
     */
    default <Y> Model<Y> join(String fromAlias, String attributeName, String alias, Predicate on) {
        From<?, ?> from = lookupFrom(fromAlias);
        return from != null ? joinFrom(from, attributeName, alias, JoinType.INNER, on)
                : join(attributeName, alias, on);
    }

    default <Y> Model<Y> leftJoin(String fromAlias, String attributeName, String alias,
            Predicate on) {
        From<?, ?> from = lookupFrom(fromAlias);
        return from != null ? joinFrom(from, attributeName, alias, JoinType.LEFT, on)
                : leftJoin(attributeName, alias, on);
    }

    default <Y> Model<Y> rightJoin(String fromAlias, String attributeName, String alias,
            Predicate on) {
        From<?, ?> from = lookupFrom(fromAlias);
        return from != null ? joinFrom(from, attributeName, alias, JoinType.RIGHT, on)
                : rightJoin(attributeName, alias, on);
    }

    /** The join starts from a table named either by its alias or by the entity behind it. */
    private From<?, ?> lookupFrom(String aliasOrClassName) {
        if (aliasOrClassName == null) {
            return null;
        }
        From<?, ?> from = getFrom(aliasOrClassName);
        if (from == null) {
            String alias = aliasOf(aliasOrClassName);
            from = alias != null ? getFrom(alias) : null;
        }
        return from;
    }

    // The new join hangs on this model rather than on the one it starts from, so that the
    // attributes of both branches keep resolvable
    @SuppressWarnings({"unchecked", "rawtypes"})
    private <Y> Model<Y> joinFrom(From<?, ?> from, String attributeName, String alias,
            JoinType joinType, Predicate on) {
        Join<?, Y> join = from.join(attributeName, joinType);
        if (on != null) {
            join.on(on);
        }
        return new JoinModel(join, alias, getMetamodel(), this);
    }

    <Y> Model<Y> join(Class<Y> joinClass, String alias, Predicate on);

    <Y> Model<Y> join(String attributeName, String alias, Predicate on);

    <Y> Model<Y> leftJoin(Class<Y> joinClass, String alias, Predicate on);

    <Y> Model<Y> leftJoin(String attributeName, String alias, Predicate on);

    <Y> Model<Y> rightJoin(Class<Y> joinClass, String alias, Predicate on);

    <Y> Model<Y> rightJoin(String attributeName, String alias, Predicate on);

    <S> Model<S> sibling(Model<S> sibling);

    static <X> Model<X> forRoot(Root<X> root) {
        return forRoot(root, ROOT);
    }

    static <X> Model<X> forRoot(Root<X> root, String alias) {
        return new RootModel<X>(root, alias, null);
    }

}
