
package com.github.easyjpa;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;

/**
 * 
 * OrFilter represents 'not' operation
 * 
 * @Author: Fred Feng
 * @Date: 23/10/2024
 * @Version 1.0.0
 */
public class NotFilter extends LogicalFilter {

    private final Filter filter;

    public NotFilter(Filter filter) {
        this.filter = filter;
    }

    @Override
    public Predicate toPredicate(Model<?> model, CriteriaBuilder builder) {
        return filter.toPredicate(model, builder).not();
    }

}
