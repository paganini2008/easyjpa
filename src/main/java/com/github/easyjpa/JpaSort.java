package com.github.easyjpa;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;

/**
 * 
 * @Description: JpaSort
 * @Author: Fred Feng
 * @Date: 18/10/2024
 * @Version 1.0.0
 */
@FunctionalInterface
public interface JpaSort {

    Order toOrder(Model<?> model, CriteriaBuilder builder);

    static JpaSort asc(String attributeName) {
        return asc(Property.forName(null, attributeName));
    }

    static JpaSort asc(String alias, String attributeName) {
        return asc(Property.forName(alias, attributeName));
    }

    static <X> JpaSort asc(SerializedFunction<X, ?> sf) {
        return asc(Property.forName(sf));
    }

    static <X> JpaSort asc(Integer number) {
        return asc(Fields.toInteger(number));
    }

    static JpaSort asc(Field<?> field) {
        return new JpaSort() {

            public Order toOrder(Model<?> model, CriteriaBuilder builder) {
                return builder.asc(field.toExpression(model, builder));
            }
        };
    }

    static JpaSort desc(String attributeName) {
        return desc(Property.forName(null, attributeName));
    }

    static JpaSort desc(String alias, String attributeName) {
        return desc(Property.forName(alias, attributeName));
    }

    static <X> JpaSort desc(SerializedFunction<X, ?> sf) {
        return desc(Property.forName(sf));
    }

    static <X> JpaSort desc(Integer number) {
        return desc(Fields.toInteger(number));
    }

    static JpaSort desc(Field<?> field) {
        return new JpaSort() {

            public Order toOrder(Model<?> model, CriteriaBuilder builder) {
                return builder.desc(field.toExpression(model, builder));
            }
        };
    }

}
