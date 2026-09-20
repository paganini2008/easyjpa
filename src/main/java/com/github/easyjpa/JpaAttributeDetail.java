
package com.github.easyjpa;

/**
 * 
 * Metadata of an entity attribute.
 * 
 * @Description: JpaAttributeDetail
 * @Author: Fred Feng
 * @Date: 18/10/2024
 * @Version 1.0.0
 */
public interface JpaAttributeDetail {

    String getName();

    Class<?> getJavaType();

    boolean isId();

    boolean isVersion();

    boolean isOptional();

    boolean isAssociation();

    boolean isCollection();

}
