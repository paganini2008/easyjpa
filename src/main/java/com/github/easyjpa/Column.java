
package com.github.easyjpa;

import java.util.List;
import com.github.easyjpa.LambdaUtils.LambdaInfo;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.criteria.Subquery;

/**
 * 
 * Represent a column of row
 * 
 * @Author: Fred Feng
 * @Date: 18/10/2024
 * @Version 1.0.0
 */
@FunctionalInterface
public interface Column {

    /** A Column is an aliased select item, whereas a {@link Field} is a bare expression. */

    Selection<?> toSelection(Model<?> model, CriteriaBuilder builder);

    static Column forName(String attributeName) {
        return forName(null, attributeName);
    }

    static Column forName(String alias, String attributeName) {
        return forName(alias, attributeName, null);
    }

    static Column forName(String attributeName, Class<?> requiredType) {
        return forName(null, attributeName, requiredType);
    }

    static Column forName(String alias, String attributeName, Class<?> requiredType) {
        return Property.forName(alias, attributeName, requiredType).as(attributeName);
    }

    @SuppressWarnings("unchecked")
    static <E, T> Column forName(SerializableFunction<E, ?> sf, Class<T> requiredType) {
        LambdaInfo info = LambdaUtils.inspect(sf);
        return Property.<E, T>forName(sf, requiredType).as(info.getAttributeName());
    }

    static Column forSubQuery(SubQueryBuilder<?> subQueryBuilder) {
        return forSubQuery(subQueryBuilder, null);
    }

    /** Select a scalar subquery as one column, which has to return a single value. */
    static Column forSubQuery(SubQueryBuilder<?> subQueryBuilder, String alias) {
        return new Column() {

            @Override
            public Selection<?> toSelection(Model<?> model, CriteriaBuilder builder) {
                Subquery<?> subquery = subQueryBuilder.toSubquery(builder);
                return alias != null ? subquery.alias(alias) : subquery;
            }
        };
    }

    static Column construct(Class<?> resultClass, String alias, String[] attributeNames) {
        return new Column() {

            @Override
            public Selection<?> toSelection(Model<?> model, CriteriaBuilder builder) {
                List<Selection<?>> selections = model.getSelections(alias, attributeNames);
                return builder.construct(resultClass, selections.toArray(new Selection[0]));
            }
        };
    }

}
