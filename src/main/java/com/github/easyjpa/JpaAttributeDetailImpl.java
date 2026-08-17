package com.github.easyjpa;

import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.SingularAttribute;

/**
 * 
 * Default JpaAttributeDetail implementation.
 * 
 * @Description: JpaAttributeDetailImpl
 * @Author: Fred Feng
 * @Date: 07/10/2024
 * @Version 1.0.0
 */
public class JpaAttributeDetailImpl<E> implements JpaAttributeDetail {

    private final Attribute<? super E, ?> delegate;

    JpaAttributeDetailImpl(Attribute<? super E, ?> delegate) {
        this.delegate = delegate;
    }

    public String getName() {
        return delegate.getName();
    }

    public Class<?> getJavaType() {
        return delegate.getJavaType();
    }

    public boolean isId() {
        return delegate instanceof SingularAttribute ? ((SingularAttribute<? super E, ?>) delegate).isId() : false;
    }

    public boolean isVersion() {
        return delegate instanceof SingularAttribute ? ((SingularAttribute<? super E, ?>) delegate).isVersion() : false;
    }

    public boolean isOptional() {
        return delegate instanceof SingularAttribute ? ((SingularAttribute<? super E, ?>) delegate).isOptional() : false;
    }

    public boolean isAssociation() {
        return delegate.isAssociation();
    }

    public boolean isCollection() {
        return delegate.isCollection();
    }

}
