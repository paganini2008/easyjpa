
package com.github.easyjpa;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

/**
 * 
 * Callback which builds a CriteriaQuery.
 * 
 * @Description: JpaQueryCallback
 * @Author: Fred Feng
 * @Date: 18/10/2024
 * @Version 1.0.0
 */
public interface JpaQueryCallback<T> {

    CriteriaQuery<T> doInJpa(CriteriaBuilder builder);

}
