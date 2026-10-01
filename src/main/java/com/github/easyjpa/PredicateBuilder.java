package com.github.easyjpa;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

/**
 * 
 * Build a Predicate from the expression of a Field.
 * 
 * @Description: PredicateBuilder
 * @Author: Fred Feng
 * @Date: 07/10/2024
 * @Version 1.0.0
 */
@FunctionalInterface
public interface PredicateBuilder<T> {

    default String getDefaultAlias() {
        return Model.ROOT;
    }

    Predicate toPredicate(Model<?> model, Expression<T> expression, CriteriaBuilder builder);

}
