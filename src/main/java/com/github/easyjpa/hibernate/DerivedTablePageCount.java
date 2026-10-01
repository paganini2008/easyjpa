
package com.github.easyjpa.hibernate;

import org.hibernate.query.criteria.JpaCriteriaQuery;
import com.github.easyjpa.AbstractPageCount;
import com.github.easyjpa.JpaCustomQuery;
import com.github.easyjpa.JpaPageCount;
import com.github.easyjpa.Model;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Subquery;

/**
 *
 * Counting by a derived table, that is to say, <code>select count(1) from (select 1 ...)</code>.
 *
 * <p>
 * A grouping query is what makes it necessary: <code>select count(1) ... group by ...</code>
 * returns one row per group, so counting it would mean fetching all the groups. The derived table
 * returns a single row instead, and covers the having clause as well. JPA has no support for a
 * subquery occurring in the from clause but Hibernate does, so the counting query is built as a
 * subquery from the very beginning, and only turns into a derived table when it gets executed.
 * </p>
 *
 * @Description: DerivedTablePageCount
 * @Author: Fred Feng
 * @Date: 17/08/2026
 * @Version 1.0.0
 */
public class DerivedTablePageCount<E> extends AbstractPageCount<E> {

    /** A subquery occurring in the from clause requires its select items to be aliased. */
    private static final String SELECTION_ALIAS = "c";

    private final CriteriaQuery<Long> query;
    private final Subquery<Long> subquery;

    DerivedTablePageCount(Model<E> model, CriteriaQuery<Long> query, Subquery<Long> subquery,
            CriteriaBuilder builder) {
        super(model, subquery, builder);
        this.query = query;
        this.subquery = subquery;
    }

    @Override
    protected <X> JpaPageCount<X> sibling(Model<X> model) {
        return new DerivedTablePageCount<X>(model, query, subquery, builder);
    }

    @Override
    public long rowCount(JpaCustomQuery<?> customQuery) {
        Long result = customQuery.getSingleResult(builder -> {
            Expression<Long> selection = builder.literal(1L);
            selection.alias(SELECTION_ALIAS);
            subquery.select(selection);
            if (query.getRoots().isEmpty()) {
                ((JpaCriteriaQuery<Long>) query).from(subquery);
            }
            return query.select(builder.count(builder.toInteger(builder.literal(1))));
        });
        return result != null ? result.longValue() : 0L;
    }

}
