package com.github.easyjpa;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaUpdate;

/**
 * 
 * Callback which builds a CriteriaUpdate.
 * 
 * @Description: JpaUpdateCallback
 * @Author: Fred Feng
 * @Date: 07/10/2024
 * @Version 1.0.0
 */
public interface JpaUpdateCallback<T> {

    CriteriaUpdate<T> doInJpa(CriteriaBuilder builder);

}
