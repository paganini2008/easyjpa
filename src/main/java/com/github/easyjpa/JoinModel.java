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
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SingularAttribute;

/**
 * 
 * JoinModel represents a Join object with other Model
 * 
 * @Author: Fred Feng
 * @Date: 07/10/2024
 * @Version 1.0.0
 */
public class JoinModel<X, Y> implements Model<Y> {

    private final Join<X, Y> join;
    private final String alias;
    private final Metamodel metamodel;
    private final Model<X> model;

    JoinModel(Join<X, Y> join, String alias, Metamodel metamodel, Model<X> model) {
        this.join = join;
        this.alias = alias;
        this.metamodel = metamodel;
        this.model = model;
    }

    /** Add the on condition after the join is established, so that it can refer to this join. */
    void on(Predicate predicate) {
        join.on(predicate);
    }

    @Override
    public Class<?> getRootType() {
        return model.getRootType();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<Y> getType() {
        Attribute<?, ?> attribute = join.getAttribute();
        // A plural attribute is typed by its collection, so the entity behind it is the element
        return attribute instanceof PluralAttribute
                ? (Class<Y>) ((PluralAttribute<?, ?, ?>) attribute).getElementType().getJavaType()
                : (Class<Y>) attribute.getJavaType();
    }

    @Override
    public boolean isManaged(Class<?> type) {
        if (getType().equals(type)) {
            return true;
        }
        return model.isManaged(type);
    }

    @Override
    public EntityType<Y> getEntityType() {
        return metamodel.entity(getType());
    }

    @Override
    public boolean hasAttribute(String name, String attributeName) {
        return this.alias.equals(name) && StringUtils.isNotBlank(attributeName) ? true
                : model.hasAttribute(name, attributeName);
    }

    @Override
    public <T> Path<T> getAttribute(String attributeName) {
        return join.get(attributeName);
    }

    @Override
    public <T> Path<T> getAttribute(String name, String attributeName) {
        if (this.alias.equals(name) && StringUtils.isNotBlank(attributeName)) {
            return join.get(attributeName);
        }
        return model.getAttribute(name, attributeName);
    }

    @Override
    public Root<?> getRoot() {
        return model.getRoot();
    }

    @Override
    public From<?, ?> getFrom() {
        return join;
    }

    @Override
    public String aliasOf(String className) {
        return getType().getName().equals(className) ? alias : model.aliasOf(className);
    }

    @Override
    public From<?, ?> getFrom(String alias) {
        return this.alias.equals(alias) ? join : model.getFrom(alias);
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
        if (this.alias.equals(alias)) {
            return join.alias(alias);
        }
        return model.getSelection(alias);
    }

    @Override
    public List<Selection<?>> getSelections(String alias, String[] attributeNames) {
        if (this.alias.equals(alias)) {
            List<Selection<?>> selections = new ArrayList<>();
            if (ArrayUtils.isNotEmpty(attributeNames)) {
                for (String name : attributeNames) {
                    selections.add(join.get(name));
                }
            }
        }
        return model.getSelections(alias, attributeNames);
    }

    @Override
    public List<JpaAttributeDetail> getAttributeDetails(String alias) {
        if (this.alias.equals(alias)) {
            List<JpaAttributeDetail> details = new ArrayList<JpaAttributeDetail>();
            for (SingularAttribute<? super Y, ?> attribute : getEntityType()
                    .getSingularAttributes()) {
                details.add(new JpaAttributeDetailImpl<>(attribute));
            }
            return details;
        }
        return model.getAttributeDetails(alias);
    }

    @Override
    public boolean isAssociatedAttribute(String attribute, Class<?> clz) {
        try {
            if (getEntityType().getSingularAttributes().stream()
                    .anyMatch(sa -> sa.getName().equals(attribute) && sa.getJavaType().equals(clz)
                            && sa.isAssociation())) {
                return true;
            }
        } catch (RuntimeException e) {
            return false;
        }
        return model.isAssociatedAttribute(attribute, clz);
    }

    @Override
    public <Z> Model<Z> join(String attributeName, String name, Predicate on) {
        Join<Y, Z> join = this.join.join(attributeName, JoinType.INNER);
        if (on != null) {
            join.on(on);
        }
        return new JoinModel<Y, Z>(join, name, metamodel, this);
    }

    @Override
    public <Z> Model<Z> leftJoin(String attributeName, String name, Predicate on) {
        Join<Y, Z> join = this.join.join(attributeName, JoinType.LEFT);
        if (on != null) {
            join.on(on);
        }
        return new JoinModel<Y, Z>(join, name, metamodel, this);
    }

    @Override
    public <Z> Model<Z> rightJoin(String attributeName, String name, Predicate on) {
        Join<Y, Z> join = this.join.join(attributeName, JoinType.RIGHT);
        if (on != null) {
            join.on(on);
        }
        return new JoinModel<Y, Z>(join, name, metamodel, this);
    }

    @Override
    public <Z> Model<Z> join(Class<Z> joinClass, String alias, Predicate on) {
        Optional<SingularAttribute<? super Y, ?>> optional = getEntityType().getSingularAttributes()
                .stream().filter(sa -> sa.getJavaType().equals(joinClass) && sa.isAssociation())
                .findFirst();
        if (optional.isPresent()) {
            String attributeName = optional.get().getName();
            return join(attributeName, alias, on);
        }
        return new SiblingModel<Y, Z>(model.join(joinClass, alias, on), this);
    }

    @Override
    public <Z> Model<Z> leftJoin(Class<Z> joinClass, String alias, Predicate on) {
        Optional<SingularAttribute<? super Y, ?>> optional = getEntityType().getSingularAttributes()
                .stream().filter(sa -> sa.getJavaType().equals(joinClass) && sa.isAssociation())
                .findFirst();
        if (optional.isPresent()) {
            String attributeName = optional.get().getName();
            return leftJoin(attributeName, alias, on);
        }
        return new SiblingModel<Y, Z>(model.leftJoin(joinClass, alias, on), this);
    }

    @Override
    public <Z> Model<Z> rightJoin(Class<Z> joinClass, String alias, Predicate on) {
        Optional<SingularAttribute<? super Y, ?>> optional = getEntityType().getSingularAttributes()
                .stream().filter(sa -> sa.getJavaType().equals(joinClass) && sa.isAssociation())
                .findFirst();
        if (optional.isPresent()) {
            String attributeName = optional.get().getName();
            return rightJoin(attributeName, alias, on);
        }
        return new SiblingModel<Y, Z>(model.rightJoin(joinClass, alias, on), this);
    }

    @Override
    public <S> Model<S> sibling(Model<S> sibling) {
        return new SiblingModel<Y, S>(sibling, this);
    }

}
