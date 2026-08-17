package com.github.easyjpa;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.SingularAttribute;

/**
 * 
 * SiblingModel contains two Models that might be uncorrelated
 * 
 * @Author: Fred Feng
 * @Date: 07/10/2024
 * @Version 1.0.0
 */
public class SiblingModel<X, Y> implements Model<Y> {

    private final Model<X> model;
    private final Model<Y> sibling;

    public SiblingModel(Model<Y> sibling, Model<X> model) {
        this.sibling = sibling;
        this.model = model;
    }

    @Override
    public EntityType<Y> getEntityType() {
        return sibling.getEntityType();
    }

    @Override
    public Class<?> getRootType() {
        return sibling.getRootType();
    }

    @Override
    public Class<Y> getType() {
        return sibling.getType();
    }

    @Override
    public boolean isManaged(Class<?> type) {
        return getType().equals(type);
    }

    @Override
    public From<?, ?> getFrom() {
        return sibling.getFrom();
    }

    @Override
    public String aliasOf(String className) {
        String alias = sibling.aliasOf(className);
        return alias != null ? alias : model.aliasOf(className);
    }

    @Override
    public From<?, ?> getFrom(String alias) {
        From<?, ?> from = sibling.getFrom(alias);
        return from != null ? from : model.getFrom(alias);
    }

    @Override
    public boolean hasAttribute(String name, String attributeName) {
        return sibling.hasAttribute(name, attributeName) ? true
                : model.hasAttribute(name, attributeName);
    }

    @Override
    public <T> Path<T> getAttribute(String attributeName) {
        return sibling.getAttribute(attributeName);
    }

    @Override
    public <T> Path<T> getAttribute(String name, String attributeName) {
        if (sibling.hasAttribute(name, attributeName)) {
            return sibling.getAttribute(name, attributeName);
        }
        return model.getAttribute(name, attributeName);
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
        return sibling.getAlias();
    }

    @Override
    public Selection<?> getSelection(String alias) {
        if (sibling.getAlias().equals(alias)) {
            return sibling.getSelection(alias);
        }
        return model.getSelection(alias);
    }

    @Override
    public List<Selection<?>> getSelections(String alias, String[] attributeNames) {
        if (sibling.getAlias().equals(alias)) {
            return sibling.getSelections(alias, attributeNames);
        }
        return model.getSelections(alias, attributeNames);
    }

    @Override
    public List<JpaAttributeDetail> getAttributeDetails(String name) {
        return sibling.getAttributeDetails(name);
    }

    @Override
    public <Z> Model<Z> join(String attributeName, String alias, Predicate on) {
        Model<Z> model = this.sibling.join(attributeName, alias, on);
        return new SiblingModel<Y, Z>(model, this);
    }

    @Override
    public <Z> Model<Z> leftJoin(String attributeName, String alias, Predicate on) {
        Model<Z> model = this.sibling.leftJoin(attributeName, alias, on);
        return new SiblingModel<Y, Z>(model, this);
    }

    @Override
    public <Z> Model<Z> rightJoin(String attributeName, String alias, Predicate on) {
        Model<Z> model = this.sibling.rightJoin(attributeName, alias, on);
        return new SiblingModel<Y, Z>(model, this);
    }

    @Override
    public <Z> Model<Z> join(Class<Z> joinClass, String alias, Predicate on) {
        Optional<SingularAttribute<? super Y, ?>> optional = getEntityType().getSingularAttributes()
                .stream().filter(sa -> sa.getJavaType().equals(joinClass)).findFirst();
        if (optional.isPresent()) {
            String attributeName = optional.get().getName();
            return join(attributeName, alias, on);
        }
        return new SiblingModel<Y, Z>(model.join(joinClass, alias, on), this);
    }

    @Override
    public <Z> Model<Z> leftJoin(Class<Z> joinClass, String alias, Predicate on) {
        Optional<SingularAttribute<? super Y, ?>> optional = getEntityType().getSingularAttributes()
                .stream().filter(sa -> sa.getJavaType().equals(joinClass)).findFirst();
        if (optional.isPresent()) {
            String attributeName = optional.get().getName();
            return leftJoin(attributeName, alias, on);
        }
        return new SiblingModel<Y, Z>(model.leftJoin(joinClass, alias, on), this);
    }

    @Override
    public <Z> Model<Z> rightJoin(Class<Z> joinClass, String alias, Predicate on) {
        Optional<SingularAttribute<? super Y, ?>> optional = getEntityType().getSingularAttributes()
                .stream().filter(sa -> sa.getJavaType().equals(joinClass)).findFirst();
        if (optional.isPresent()) {
            String attributeName = optional.get().getName();
            return rightJoin(attributeName, alias, on);
        }
        return new SiblingModel<Y, Z>(model.rightJoin(joinClass, alias, on), this);
    }

    @Override
    public boolean isAssociatedAttribute(String attribute, Class<?> clz) {
        if (sibling.isAssociatedAttribute(attribute, clz)) {
            return true;
        }
        return model.isAssociatedAttribute(attribute, clz);
    }

    @Override
    public <S> Model<S> sibling(Model<S> sibling) {
        return new SiblingModel<Y, S>(sibling, this);
    }

}
