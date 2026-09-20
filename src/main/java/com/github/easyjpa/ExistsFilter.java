package com.github.easyjpa;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;

/**
 * 
 * ExistsFilter like exists statement
 * 
 * @Author: Fred Feng
 * @Date: 07/10/2024
 * @Version 1.0.0
 */
public class ExistsFilter extends LogicalFilter {

    private final SubQueryBuilder<?> queryBuiler;

    ExistsFilter(SubQueryBuilder<?> queryBuiler) {
        this.queryBuiler = queryBuiler;
    }

    public Predicate toPredicate(Model<?> selector, CriteriaBuilder builder) {
        return builder.exists(queryBuiler.toSubquery(builder));
    }

}
