package com.github.easyjpa;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;

/**
 * 
 * Create a Filter to implement where statement
 * 
 * @Author: Fred Feng
 * @Date: 07/10/2024
 * @Version 1.0.0
 */
@FunctionalInterface
public interface Filter {

    public Predicate toPredicate(Model<?> model, CriteriaBuilder builder);

}
