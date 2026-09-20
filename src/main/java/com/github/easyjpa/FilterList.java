package com.github.easyjpa;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;

/**
 * 
 * A list of Filters joined by and/or, which builds a where clause fluently.
 * 
 * @Description: FilterList
 * @Author: Fred Feng
 * @Date: 20/10/2024
 * @Version 1.0.0
 */
public class FilterList implements Filter {

    public FilterList() {}

    public FilterList(Filter... filters) {
        this(filters != null ? List.of(filters) : Collections.emptyList());
    }

    public FilterList(Collection<Filter> filters) {
        if (filters != null && filters.size() > 0) {
            filters.forEach(this::join);
        }
    }

    private LogicalFilter logicalFilter;
    private boolean andOr = true;

    public <X> FilterList eq(SerializableFunction<X, ?> function, Object value) {
        return join(Restrictions.eq(function, value));
    }

    public <X, T> FilterList eq(SerializableFunction<X, T> function, SubQueryBuilder<T> subQuery) {
        return join(Restrictions.eq(function, subQuery));
    }

    public <X, Y> FilterList eq(SerializableFunction<X, ?> leftFun,
                                SerializableFunction<Y, ?> rightFun) {
        return join(Restrictions.eq(leftFun, rightFun));
    }

    public <X> FilterList ne(SerializableFunction<X, ?> function, Object value) {
        return join(Restrictions.ne(function, value));
    }

    public <X, T> FilterList ne(SerializableFunction<X, T> function, SubQueryBuilder<T> subQuery) {
        return join(Restrictions.ne(function, subQuery));
    }

    public <X, R extends Comparable<R>> FilterList lt(SerializableFunction<X, R> function, R value) {
        return join(Restrictions.lt(function, value));
    }

    public <X, R extends Comparable<R>> FilterList lt(SerializableFunction<X, R> function,
                                                      SubQueryBuilder<R> subQuery) {
        return join(Restrictions.lt(function, subQuery));
    }

    public <X, R extends Comparable<R>> FilterList lte(SerializableFunction<X, R> function, R value) {
        return join(Restrictions.lte(function, value));
    }

    public <X, R extends Comparable<R>> FilterList lte(SerializableFunction<X, R> function,
                                                       SubQueryBuilder<R> subQuery) {
        return join(Restrictions.lte(function, subQuery));
    }

    public <X, R extends Comparable<R>> FilterList gt(SerializableFunction<X, R> function, R value) {
        return join(Restrictions.gt(function, value));
    }

    public <X, R extends Comparable<R>> FilterList gte(SerializableFunction<X, R> function, R value) {
        return join(Restrictions.gte(function, value));
    }

    public <X, T> FilterList in(SerializableFunction<X, T> function, Iterable<T> values) {
        return join(Restrictions.in(function, values));
    }

    public <X, T extends Comparable<T>> FilterList between(SerializableFunction<X, T> function,
                                                           T startValue, T endValue) {
        return join(Restrictions.between(function, startValue, endValue));
    }

    public <X> FilterList like(SerializableFunction<X, String> function, String pattern) {
        return join(Restrictions.like(function, pattern));
    }

    public <X> FilterList like(SerializableFunction<X, String> function, String pattern,
                               char escapeChar) {
        return join(Restrictions.like(function, pattern, escapeChar));
    }

    public <X> FilterList notLike(SerializableFunction<X, String> function, String pattern) {
        return join(Restrictions.notLike(function, pattern));
    }

    public <X> FilterList notLike(SerializableFunction<X, String> function, String pattern,
                                  char escapeChar) {
        return join(Restrictions.notLike(function, pattern, escapeChar));
    }

    public <X> FilterList notNull(SerializableFunction<X, ?> function) {
        return join(Restrictions.notNull(function));
    }

    public <X> FilterList isNull(SerializableFunction<X, ?> function) {
        return join(Restrictions.isNull(function));
    }

    public FilterList eq(String attributeName, Object value) {
        return join(Restrictions.eq(attributeName, value));
    }

    public FilterList eq(String alias, String attributeName, Object value) {
        return join(Restrictions.eq(alias, attributeName, value));
    }

    public FilterList eq(Field<?> field, Object value) {
        return join(Restrictions.eq(field, value));
    }

    public <X, Y> FilterList eq(Field<X> field, Field<Y> otherField) {
        return join(Restrictions.eq(field, otherField));
    }

    public <T> FilterList eq(Field<T> field, SubQueryBuilder<T> subQuery) {
        return join(Restrictions.eq(field, subQuery));
    }

    public FilterList ne(String attributeName, Object value) {
        return join(Restrictions.ne(attributeName, value));
    }

    public FilterList ne(String alias, String attributeName, Object value) {
        return join(Restrictions.ne(alias, attributeName, value));
    }

    public <T> FilterList ne(Field<T> field, Object value) {
        return join(Restrictions.ne(field, value));
    }

    public <X, Y> FilterList ne(Field<X> field, Field<Y> otherField) {
        return join(Restrictions.ne(field, otherField));
    }

    public <T extends Comparable<T>> FilterList lt(String alias, String attributeName, T value) {
        return join(Restrictions.lt(alias, attributeName, value));
    }

    public <T extends Comparable<T>> FilterList lt(Field<T> field, T value) {
        return join(Restrictions.lt(field, value));
    }

    public <T extends Comparable<T>> FilterList lt(Field<T> field, SubQueryBuilder<T> subQuery) {
        return join(Restrictions.lt(field, subQuery));
    }

    public <T extends Comparable<T>> FilterList lt(Field<T> field, Field<T> otherField) {
        return join(Restrictions.lt(field, otherField));
    }

    public <T extends Comparable<T>> FilterList lte(String alias, String attributeName, T value) {
        return join(Restrictions.lte(alias, attributeName, value));
    }

    public <T extends Comparable<T>> FilterList lte(Field<T> field, T value) {
        return join(Restrictions.lte(field, value));
    }

    public <T extends Comparable<T>> FilterList lte(Field<T> field, SubQueryBuilder<T> subQuery) {
        return join(Restrictions.lte(field, subQuery));
    }

    public <T extends Comparable<T>> FilterList lte(Field<T> field, Field<T> otherField) {
        return join(Restrictions.lte(field, otherField));
    }

    public <T extends Comparable<T>> FilterList gt(String alias, String attributeName, T value) {
        return join(Restrictions.gt(alias, attributeName, value));
    }

    public <T extends Comparable<T>> FilterList gt(Field<T> field, T value) {
        return join(Restrictions.gt(field, value));
    }

    public <T extends Comparable<T>> FilterList gt(Field<T> field, SubQueryBuilder<T> subQuery) {
        return join(Restrictions.gt(field, subQuery));
    }

    public <T extends Comparable<T>> FilterList gt(Field<T> field, Field<T> otherField) {
        return join(Restrictions.gt(field, otherField));
    }

    public <X, R extends Comparable<R>> FilterList gt(SerializableFunction<X, R> function,
                                                      SubQueryBuilder<R> subQuery) {
        return join(Restrictions.gt(function, subQuery));
    }

    public <T extends Comparable<T>> FilterList gte(String alias, String attributeName, T value) {
        return join(Restrictions.gte(alias, attributeName, value));
    }

    public <T extends Comparable<T>> FilterList gte(Field<T> field, T value) {
        return join(Restrictions.gte(field, value));
    }

    public <T extends Comparable<T>> FilterList gte(Field<T> field, SubQueryBuilder<T> subQuery) {
        return join(Restrictions.gte(field, subQuery));
    }

    public <T extends Comparable<T>> FilterList gte(Field<T> field, Field<T> otherField) {
        return join(Restrictions.gte(field, otherField));
    }

    public <X, R extends Comparable<R>> FilterList gte(SerializableFunction<X, R> function,
                                                       SubQueryBuilder<R> subQuery) {
        return join(Restrictions.gte(function, subQuery));
    }

    public <T> FilterList in(String attributeName, Iterable<T> values) {
        return join(Restrictions.in(attributeName, values));
    }

    public <T> FilterList in(String alias, String attributeName, Iterable<T> values) {
        return join(Restrictions.in(alias, attributeName, values));
    }

    public <T> FilterList in(Field<T> field, Iterable<T> values) {
        return join(Restrictions.in(field, values));
    }

    public <T> FilterList in(Field<T> field, SubQueryBuilder<T> subQuery) {
        return join(Restrictions.in(field, subQuery));
    }

    public <X, T> FilterList in(SerializableFunction<X, T> function, SubQueryBuilder<T> subQuery) {
        return join(Restrictions.in(function, subQuery));
    }

    public <T extends Comparable<T>> FilterList between(String alias, String attributeName,
            T startValue, T endValue) {
        return join(Restrictions.between(alias, attributeName, startValue, endValue));
    }

    public <T extends Comparable<T>> FilterList between(Field<T> field, T startValue, T endValue) {
        return join(Restrictions.between(field, startValue, endValue));
    }

    public FilterList like(String alias, String attributeName, String pattern) {
        return join(Restrictions.like(alias, attributeName, pattern));
    }

    public FilterList like(Field<String> field, String pattern) {
        return join(Restrictions.like(field, pattern));
    }

    public FilterList notLike(String alias, String attributeName, String pattern) {
        return join(Restrictions.notLike(alias, attributeName, pattern));
    }

    public FilterList notLike(Field<String> field, String pattern) {
        return join(Restrictions.notLike(field, pattern));
    }

    public FilterList isNull(String attributeName) {
        return join(Restrictions.isNull(attributeName));
    }

    public FilterList isNull(String alias, String attributeName) {
        return join(Restrictions.isNull(alias, attributeName));
    }

    public <T> FilterList isNull(Field<T> field) {
        return join(Restrictions.isNull(field));
    }

    public FilterList notNull(String attributeName) {
        return join(Restrictions.notNull(attributeName));
    }

    public FilterList notNull(String alias, String attributeName) {
        return join(Restrictions.notNull(alias, attributeName));
    }

    public <T> FilterList notNull(Field<T> field) {
        return join(Restrictions.notNull(field));
    }

    public FilterList exists(SubQueryBuilder<?> subQuery) {
        return join(Restrictions.exists(subQuery));
    }

    public FilterList not() {
        logicalFilter = logicalFilter.not();
        return this;
    }

    public FilterList and() {
        andOr = true;
        return this;
    }

    public FilterList or() {
        andOr = false;
        return this;
    }

    public FilterList and(Supplier<FilterList> supplier) {
        andOr = true;
        return join(supplier.get());
    }

    public FilterList or(Supplier<FilterList> supplier) {
        andOr = false;
        return join(supplier.get());
    }

    public FilterList join(Filter filter) {
        if (logicalFilter != null) {
            logicalFilter = andOr ? logicalFilter.and(filter) : logicalFilter.or(filter);
        } else {
            logicalFilter = filter instanceof LogicalFilter ? (LogicalFilter) filter
                    : andOr ? Restrictions.juction().and(filter)
                            : Restrictions.disjuction().or(filter);
        }
        return this;
    }


    @Override
    public Predicate toPredicate(Model<?> model, CriteriaBuilder builder) {
        return logicalFilter.toPredicate(model, builder);
    }

}
