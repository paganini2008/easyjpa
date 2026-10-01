
package com.github.easyjpa;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Selection;

/**
 * 
 * Field is used for group, sort statement or function in columns
 * 
 * @Author: Fred Feng
 * @Date: 18/10/2024
 * @Version 1.0.0
 */
@FunctionalInterface
public interface Field<T> {

    Expression<T> toExpression(Model<?> model, CriteriaBuilder builder);

    default Column as(final String alias) {
        return new Column() {

            public Selection<?> toSelection(Model<?> model, CriteriaBuilder builder) {
                return Field.this.toExpression(model, builder).alias(alias);
            }

            public String toString() {
                return Field.this.toString();
            }
        };
    }
}
