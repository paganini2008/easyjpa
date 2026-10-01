package com.github.easyjpa;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.SingularAttribute;

/**
 * 
 * RootModel represents current object
 * 
 * @Author: Fred Feng
 * @Date: 07/10/2024
 * @Version 1.0.0
 */
public class RootModel<X> implements Model<X> {

    private final Root<X> root;
    private final String alias;
    private final Metamodel metamodel;

    public RootModel(Root<X> root, String alias, Metamodel metamodel) {
        this.root = root;
        this.alias = alias;
        this.metamodel = metamodel;
    }

    @Override
    public boolean hasAttribute(String name, String attributeName) {
        return this.alias.equals(name) && StringUtils.isNotBlank(attributeName);
    }

    @Override
    public <T> Path<T> getAttribute(String attributeName) {
        return getAttribute(ROOT, attributeName);
    }

    @Override
    public <T> Path<T> getAttribute(String name, String attributeName) {
        if (hasAttribute(name, attributeName)) {
            return PathUtils.createPath(root, attributeName);
        }
        throw new PathMismatchedException(name, attributeName);
    }

    @Override
    public Class<?> getRootType() {
        return getType();
    }

    @Override
    public Class<X> getType() {
        return root.getModel().getBindableJavaType();
    }

    @Override
    public EntityType<X> getEntityType() {
        return root != null ? root.getModel() : null;
    }

    @Override
    public boolean isManaged(Class<?> type) {
        return getType().equals(type);
    }

    @Override
    public <Y> Model<Y> join(String attributeName, String alias, Predicate on) {
        Join<X, Y> join = root.join(attributeName, JoinType.INNER);
        if (on != null) {
            join.on(on);
        }
        return new JoinModel<X, Y>(join, alias, metamodel, this);
    }

    @Override
    public <Y> Model<Y> leftJoin(String attributeName, String alias, Predicate on) {
        Join<X, Y> join = root.join(attributeName, JoinType.LEFT);
        if (on != null) {
            join.on(on);
        }
        return new JoinModel<X, Y>(join, alias, metamodel, this);
    }

    @Override
    public <Y> Model<Y> rightJoin(String attributeName, String alias, Predicate on) {
        Join<X, Y> join = root.join(attributeName, JoinType.RIGHT);
        if (on != null) {
            join.on(on);
        }
        return new JoinModel<X, Y>(join, alias, metamodel, this);
    }

    @Override
    public <Y> Model<Y> join(Class<Y> joinClass, String alias, Predicate on) {
        Optional<SingularAttribute<? super X, ?>> optional = getEntityType().getSingularAttributes()
                .stream().filter(sa -> sa.getJavaType().equals(joinClass) && sa.isAssociation())
                .findFirst();
        if (optional.isEmpty()) {
            throw new IllegalStateException(String.format("Uncorrelated class '%s' in '%s'",
                    joinClass.getName(), getType()));
        }
        String attributeName = optional.get().getName();
        return join(attributeName, alias, on);
    }

    @Override
    public <Y> Model<Y> leftJoin(Class<Y> joinClass, String alias, Predicate on) {
        Optional<SingularAttribute<? super X, ?>> optional = getEntityType().getSingularAttributes()
                .stream().filter(sa -> sa.getJavaType().equals(joinClass) && sa.isAssociation())
                .findFirst();
        if (optional.isEmpty()) {
            throw new IllegalStateException(String.format("Uncorrelated class '%s' in '%s'",
                    joinClass.getName(), getType()));
        }
        String attributeName = optional.get().getName();
        return leftJoin(attributeName, alias, on);
    }

    @Override
    public <Y> Model<Y> rightJoin(Class<Y> joinClass, String alias, Predicate on) {
        Optional<SingularAttribute<? super X, ?>> optional = getEntityType().getSingularAttributes()
                .stream().filter(sa -> sa.getJavaType().equals(joinClass) && sa.isAssociation())
                .findFirst();
        if (optional.isEmpty()) {
            throw new IllegalStateException(String.format("Uncorrelated class '%s' in '%s'",
                    joinClass.getName(), getType()));
        }
        String attributeName = optional.get().getName();
        return rightJoin(attributeName, alias, on);
    }

    @Override
    public Root<X> getRoot() {
        return root;
    }

    @Override
    public From<?, ?> getFrom() {
        return root;
    }

    @Override
    public String aliasOf(String className) {
        return getType().getName().equals(className) ? alias : null;
    }

    @Override
    public From<?, ?> getFrom(String alias) {
        return this.alias.equals(alias) ? root : null;
    }

    @Override
    public Metamodel getMetamodel() {
        return metamodel;
    }

    @Override
    public String getAlias() {
        return alias;
    }

    @Override
    public Selection<?> getSelection(String alias) {
        if (!this.alias.equals(alias)) {
            throw new AliasMismatchedException(alias);
        }
        return root.alias(alias);
    }

    @Override
    public List<Selection<?>> getSelections(String alias, String[] attributeNames) {
        if (!this.alias.equals(alias)) {
            throw new AliasMismatchedException(alias);
        }
        List<Selection<?>> selections = new ArrayList<>();
        if (ArrayUtils.isNotEmpty(attributeNames)) {
            for (String name : attributeNames) {
                selections.add(root.get(name));
            }
        }
        return selections;
    }

    @Override
    public List<JpaAttributeDetail> getAttributeDetails(String name) {
        if (!this.alias.equals(name)) {
            throw new AliasMismatchedException(name);
        }
        List<JpaAttributeDetail> details = new ArrayList<JpaAttributeDetail>();
        for (SingularAttribute<? super X, ?> attribute : root.getModel().getSingularAttributes()) {
            details.add(new JpaAttributeDetailImpl<>(attribute));
        }
        return details;
    }

    @Override
    public boolean isAssociatedAttribute(String attribute, Class<?> clz) {
        return root.getModel().getSingularAttributes().stream()
                .anyMatch(sa -> sa.getName().equals(attribute) && sa.getJavaType().equals(clz)
                        && sa.isAssociation());
    }

    @Override
    public <S> Model<S> sibling(Model<S> sibling) {
        return new SiblingModel<X, S>(sibling, this);
    }

}
