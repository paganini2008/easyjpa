
package com.github.easyjpa;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

/**
 * 
 * Filter which turns a Field into a Predicate.
 * 
 * @Description: FieldFilter
 * @Author: Fred Feng
 * @Date: 18/10/2024
 * @Version 1.0.0
 */
public class FieldFilter<T> extends LogicalFilter {

    private final Field<T> field;
    private final PredicateBuilder<T> builder;

    FieldFilter(Field<T> field, PredicateBuilder<T> builder) {
        this.field = field;
        this.builder = builder;
    }

    public Predicate toPredicate(Model<?> model, CriteriaBuilder cb) {
        final Expression<T> expression = field.toExpression(model, cb);
        return builder.toPredicate(model, expression, cb);
    }

}
