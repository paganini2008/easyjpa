
package com.github.easyjpa;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;

/**
 * 
 * OrFilter represents 'and' statement
 * 
 * @Author: Fred Feng
 * @Date: 20/10/2024
 * @Version 1.0.0
 */
public class OrFilter extends LogicalFilter {

    private final Filter filter;

    private final Filter otherFilter;

    public OrFilter(Filter filter, Filter otherFilter) {
        this.filter = filter;
        this.otherFilter = otherFilter;
    }

    @Override
    public Predicate toPredicate(Model<?> selector, CriteriaBuilder builder) {
        Predicate left = filter.toPredicate(selector, builder);
        Predicate right = otherFilter.toPredicate(selector, builder);
        return builder.or(left, right);
    }

}
