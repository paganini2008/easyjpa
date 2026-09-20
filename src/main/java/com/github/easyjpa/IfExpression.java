package com.github.easyjpa;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaBuilder.SimpleCase;
import jakarta.persistence.criteria.Expression;

/**
 * 
 * Case when clause
 * 
 * @Author: Fred Feng
 * @Date: 07/10/2024
 * @Version 1.0.0
 */
public class IfExpression<T, R> implements Field<R> {

    private final Field<T> field;
    private final List<T> conditions = new ArrayList<T>();
    private final List<R> results = new ArrayList<R>();
    private final List<Field<R>> resultFields = new ArrayList<Field<R>>();
    private R defaultResult;
    private Field<R> defaultFieldResult;

    public IfExpression(String attributeName) {
        this(Property.forName(null, attributeName));
    }

    public IfExpression(String alias, String attributeName) {
        this(Property.forName(alias, attributeName));
    }

    public <X> IfExpression(SerializableFunction<X, T> function) {
        this.field = Property.forName(function);
    }

    public IfExpression(Field<T> field) {
        this.field = field;
    }

    public IfExpression<T, R> when(T condition, R result) {
        conditions.add(condition);
        results.add(result);
        resultFields.add(null);
        return this;
    }

    public IfExpression<T, R> when(T condition, Field<R> result) {
        conditions.add(condition);
        results.add(null);
        resultFields.add(result);
        return this;
    }

    public IfExpression<T, R> otherwise(R result) {
        this.defaultResult = result;
        return this;
    }

    public IfExpression<T, R> otherwise(Field<R> otherwise) {
        this.defaultFieldResult = otherwise;
        return this;
    }

    public Expression<R> toExpression(Model<?> model, CriteriaBuilder builder) {
        SimpleCase<T, R> theCase = builder.selectCase(field.toExpression(model, builder));
        for (int i = 0, l = conditions.size(); i < l; i++) {
            R result = results.get(i);
            if (result != null) {
                theCase = theCase.when(conditions.get(i), result);
            } else if (resultFields.get(i) != null) {
                theCase = theCase.when(conditions.get(i),
                        resultFields.get(i).toExpression(model, builder));
            }
        }
        if (defaultResult != null) {
            return theCase.otherwise(defaultResult);
        } else if (defaultFieldResult != null) {
            return theCase.otherwise(defaultFieldResult.toExpression(model, builder));
        }
        throw new UnsupportedOperationException(field.toString());
    }

}
