
package com.github.easyjpa;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;

/**
 * 
 * Callback which builds a CriteriaDelete.
 * 
 * @Description: JpaDeleteCallback
 * @Author: Fred Feng
 * @Date: 18/10/2024
 * @Version 1.0.0
 */
public interface JpaDeleteCallback<T> {

    CriteriaDelete<T> doInJpa(CriteriaBuilder builder);

}
