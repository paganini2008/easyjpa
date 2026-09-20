package com.github.easyjpa;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaBuilder.Case;
import jakarta.persistence.criteria.Expression;

/**
 * 
 * Case when clause
 * 
 * @Author: Fred Feng
 * @Date: 07/10/2024
 * @Version 1.0.0
 */
public class CaseWhenExpression<R> implements Field<R> {

    private final List<Field<Boolean>> conditions = new ArrayList<>();
    private final List<R> results = new ArrayList<R>();
    private final List<Field<R>> resultFields = new ArrayList<Field<R>>();
    private R defaultResult;
    private Field<R> defaultFieldResult;

    public CaseWhenExpression<R> when(Field<Boolean> condition, R result) {
        conditions.add(condition);
        results.add(result);
        resultFields.add(null);
        return this;
    }

    public CaseWhenExpression<R> when(Field<Boolean> condition, Field<R> result) {
        conditions.add(condition);
        results.add(null);
        resultFields.add(result);
        return this;
    }

    public CaseWhenExpression<R> otherwise(R result) {
        this.defaultResult = result;
        return this;
    }

    public CaseWhenExpression<R> otherwise(Field<R> otherwise) {
        this.defaultFieldResult = otherwise;
        return this;
    }

    public Expression<R> toExpression(Model<?> model, CriteriaBuilder builder) {
        Case<R> theCase = builder.selectCase();
        for (int i = 0, l = conditions.size(); i < l; i++) {
            R result = results.get(i);
            if (result != null) {
                theCase = theCase.when(conditions.get(i).toExpression(model, builder), result);
            } else if (resultFields.get(i) != null) {
                theCase = theCase.when(conditions.get(i).toExpression(model, builder),
                        resultFields.get(i).toExpression(model, builder));
            }
        }
        if (defaultResult != null) {
            return theCase.otherwise(defaultResult);
        } else if (defaultFieldResult != null) {
            return theCase.otherwise(defaultFieldResult.toExpression(model, builder));
        }
        throw new UnsupportedOperationException();
    }

}
