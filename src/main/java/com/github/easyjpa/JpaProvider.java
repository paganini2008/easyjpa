
package com.github.easyjpa;

import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.criteria.Subquery;

/**
 *
 * What a JPA provider is asked for whenever the Criteria API alone does not reach far enough. One
 * implementation stands for one provider, and the whole library talks to this interface rather
 * than to any provider directly.
 *
 * @Description: JpaProvider
 * @Author: Fred Feng
 * @Date: 17/08/2026
 * @Version 1.0.0
 */
public interface JpaProvider {

    /** The name shown when something is not supported here. */
    String getName();

    /**
     * Whether a subquery may occur in the from clause. It is what a pagination counts the groups
     * by, and what {@link JpaQuery#joinSubQuery} joins, so a provider without it counts the groups
     * another way and rejects that join.
     */
    default boolean supportsDerivedTable() {
        return false;
    }

    /**
     * Whether several group-by keys may be concatenated into one, which is how the groups are
     * counted distinctly. A provider unable to build such an expression counts those groups one by
     * one instead.
     */
    default boolean supportsConcatenatedGroupKey() {
        return true;
    }

    /**
     * Whether the parts of a date are rendered the way every database spells them. The Criteria
     * API defines no such function, so without the provider it comes down to whatever function the
     * database happens to name that way.
     */
    default boolean supportsDatePart() {
        return false;
    }

    /** Whether a right join is rendered at all. */
    default boolean supportsRightJoin() {
        return true;
    }

    /** Whether a subquery may stand on one side of a comparison, rather than being quantified. */
    default boolean supportsSubQueryAsExpression() {
        return true;
    }

    /** Whether a subquery may be selected as one column of the result. */
    default boolean supportsSubQueryAsSelection() {
        return true;
    }

    /**
     * Whether an entity may be selected column by column. A provider caching what it reads may
     * refuse to hand out an entity only partly filled in.
     */
    default boolean supportsPartialEntity() {
        return true;
    }

    /** Whether an order by may name a column by its position in the select list. */
    default boolean supportsOrdinalSort() {
        return true;
    }

    /** Whether a function of the database itself is passed through as it is written. */
    default boolean supportsPassThroughFunction() {
        return true;
    }

    /** Whether a comparison may be selected as one column, which yields a boolean. */
    default boolean supportsComparisonAsSelection() {
        return true;
    }

    /** Whether the columns are written into a bean by its properties instead of its constructor. */
    default boolean supportsBeanProjection() {
        return true;
    }

    /** The dao implementation, which is where the native sql mapping differs by provider. */
    <E, ID> EntityDaoSupport<E, ID> createDaoSupport(Class<E> entityClass, EntityManager em);

    /** The base class Spring Data derives every repository from. */
    Class<?> getDaoSupportClass();

    /**
     * The counting query of a pagination, whose shape depends on how this provider counts the
     * groups of a grouping query.
     */
    <E> JpaPageCount<E> createPageCount(Class<E> entityClass, String alias, EntityManager em);

    /** Join a subquery as a derived table, which not every provider supports. */
    <X> Model<X> joinSubQuery(Model<X> model, Subquery<?> subquery, String alias,
            JoinType joinType, Filter on, CriteriaBuilder builder);

    /** Select several columns in a subquery, which not every provider supports. */
    void multiselect(Subquery<?> subquery, List<Selection<?>> selections);

    /** The year, the month or the day of a date, which every database spells its own way. */
    Expression<Integer> extract(CriteriaBuilder builder, Expression<?> expression, DatePart part);

    /**
     * An expression turned into text by the database itself.
     *
     * <p>
     * {@link Expression#as(Class)} casts in the type system of Java and no further: a provider is
     * free to render the column exactly as it stands, and Hibernate does. A database that widens
     * whatever it is handed to text lets that pass, while one that does not, SQL Server among
     * them, refuses to concatenate a date or a number it was never given as text. A provider able
     * to write a real cast says so here.
     * </p>
     */
    default Expression<String> asText(CriteriaBuilder builder, Expression<?> expression) {
        return expression.as(String.class);
    }

    /** The part of a date to extract. */
    enum DatePart {
        YEAR, MONTH, DAY
    }

}
