package com.github.easyjpa;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;

/**
 * 
 * AndFilter represents 'and' statement
 * 
 * @Author: Fred Feng
 * @Date: 07/10/2024
 * @Version 1.0.0
 */
public class AndFilter extends LogicalFilter {

    private final Filter leftFilter;

    private final Filter rightFilter;

    public AndFilter(Filter leftFilter, Filter rightFilter) {
        this.leftFilter = leftFilter;
        this.rightFilter = rightFilter;
    }

    @Override
    public Predicate toPredicate(Model<?> model, CriteriaBuilder builder) {
        Predicate leftPredicate = leftFilter.toPredicate(model, builder);
        Predicate rightPredicate = rightFilter.toPredicate(model, builder);
        return builder.and(leftPredicate, rightPredicate);
    }

}
