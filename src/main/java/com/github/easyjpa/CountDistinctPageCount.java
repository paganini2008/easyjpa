
package com.github.easyjpa;

import java.time.temporal.Temporal;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;

/**
 *
 * Counting with nothing but the Criteria API, which is what a provider without derived tables is
 * left with.
 *
 * <p>
 * A <code>select count(1) ... group by ...</code> statement returns one row per group, and the
 * groups are as many as the distinct group-by keys, so the grouping clause is dropped and the
 * selection is rewritten as a <code>count(distinct key)</code>. A having clause filters the groups
 * after aggregating, which no such rewriting expresses, and there the rows have to be counted one
 * by one.
 * </p>
 *
 * @Description: CountDistinctPageCount
 * @Author: Fred Feng
 * @Date: 17/08/2026
 * @Version 1.0.0
 */
public class CountDistinctPageCount<E> extends AbstractPageCount<E> {

    /** Non printable characters, which are hardly used in the real data. */
    private static final String KEY_SEPARATOR = String.valueOf((char) 1);
    private static final String NULL_PLACEHOLDER = String.valueOf((char) 2);

    private final CriteriaQuery<Long> query;

    CountDistinctPageCount(Model<E> model, CriteriaQuery<Long> query, CriteriaBuilder builder) {
        super(model, query, builder);
        this.query = query;
    }

    @Override
    protected <X> JpaPageCount<X> sibling(Model<X> model) {
        return new CountDistinctPageCount<X>(model, query, builder);
    }

    @Override
    public long rowCount(JpaCustomQuery<?> customQuery) {
        List<Expression<?>> groupList = query.getGroupList();
        if (groupList.isEmpty()) {
            return count(customQuery,
                    builder -> query.select(builder.count(builder.toInteger(builder.literal(1)))));
        }
        if (query.getGroupRestriction() != null || !canCountDistinctly(groupList)) {
            // Either a having clause is present or the keys can not be concatenated safely
            List<Long> list = customQuery.getResultList(
                    builder -> query.select(builder.count(builder.toInteger(builder.literal(1)))));
            return list != null ? list.size() : 0L;
        }
        List<Expression<?>> groups = List.copyOf(groupList);
        try {
            return count(customQuery, builder -> {
                query.groupBy();
                return query.select(getGroupCount(builder, groups));
            });
        } finally {
            // Keep the counting query reusable
            query.groupBy(groups);
        }
    }

    private long count(JpaCustomQuery<?> customQuery, JpaQueryCallback<Long> callback) {
        Long result = customQuery.getSingleResult(callback);
        return result != null ? result.longValue() : 0L;
    }

    private boolean canCountDistinctly(List<Expression<?>> groupList) {
        if (groupList.size() == 1) {
            return true;
        }
        if (!JpaProviders.getProvider().supportsConcatenatedGroupKey()) {
            return false;
        }
        // Multiple keys have to be concatenated into one string, which only the basic types allow
        for (Expression<?> expression : groupList) {
            if (!isSimpleType(expression.getJavaType())) {
                return false;
            }
        }
        return true;
    }

    private Expression<Long> getGroupCount(CriteriaBuilder builder,
            List<Expression<?>> groupList) {
        if (groupList.size() == 1) {
            Expression<?> groupKey = groupList.get(0);
            // count(distinct x) skips the null values whereas 'group by x' treats null as a group
            // of its own, so that group, if any, is counted apart
            Expression<Long> nullGroup = builder.<Long>selectCase()
                    .when(builder.gt(builder.count(builder.toInteger(builder.literal(1))),
                            builder.count(groupKey)), 1L)
                    .otherwise(0L).as(Long.class);
            return builder.sum(builder.countDistinct(groupKey), nullGroup);
        }
        Expression<String> groupKey = null;
        for (Expression<?> expression : groupList) {
            // Concatenating the key with the separator makes it evaluated as a string, whatever
            // the underlying column is, so the placeholder of a null value binds as a string too
            Expression<String> part =
                    builder.coalesce(builder.concat(KEY_SEPARATOR, getStringExpression(expression)),
                            KEY_SEPARATOR + NULL_PLACEHOLDER);
            groupKey = groupKey != null ? builder.concat(groupKey, part) : part;
        }
        return builder.countDistinct(groupKey);
    }

    @SuppressWarnings("unchecked")
    private Expression<String> getStringExpression(Expression<?> expression) {
        return CharSequence.class.isAssignableFrom(expression.getJavaType())
                ? (Expression<String>) expression
                : JpaProviders.getProvider().asText(builder, expression);
    }

    private boolean isSimpleType(Class<?> javaType) {
        return javaType != null && (javaType.isPrimitive() || javaType.isEnum()
                || CharSequence.class.isAssignableFrom(javaType)
                || Number.class.isAssignableFrom(javaType) || Boolean.class == javaType
                || Character.class == javaType || Date.class.isAssignableFrom(javaType)
                || Temporal.class.isAssignableFrom(javaType) || UUID.class == javaType);
    }

}
