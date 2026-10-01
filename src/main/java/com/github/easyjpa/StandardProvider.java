
package com.github.easyjpa;

import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.criteria.Subquery;

/**
 *
 * What the Criteria API alone can do, which is where an unknown provider lands. A derived table is
 * beyond it, so a grouping pagination is counted by a count(distinct ...) instead, and a date part
 * is extracted by the function the database happens to name that way.
 *
 * @Description: StandardProvider
 * @Author: Fred Feng
 * @Date: 17/08/2026
 * @Version 1.0.0
 */
public class StandardProvider implements JpaProvider {

    @Override
    public String getName() {
        return "Jakarta Persistence";
    }

    @Override
    public <E, ID> EntityDaoSupport<E, ID> createDaoSupport(Class<E> entityClass,
            EntityManager em) {
        return new StandardDaoSupport<E, ID>(entityClass, em);
    }

    @Override
    public Class<?> getDaoSupportClass() {
        return StandardDaoSupport.class;
    }

    @Override
    public <E> JpaPageCount<E> createPageCount(Class<E> entityClass, String alias,
            EntityManager em) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<E> root = query.from(entityClass);
        return new CountDistinctPageCount<E>(new RootModel<E>(root, alias, em.getMetamodel()),
                query, builder);
    }

    @Override
    public <X> Model<X> joinSubQuery(Model<X> model, Subquery<?> subquery, String alias,
            JoinType joinType, Filter on, CriteriaBuilder builder) {
        throw new UnsupportedOperationException(
                "Joining a subquery as a derived table is beyond " + getName());
    }

    @Override
    public void multiselect(Subquery<?> subquery, List<Selection<?>> selections) {
        throw new UnsupportedOperationException(
                "Selecting several columns in a subquery is beyond " + getName());
    }

    /**
     * Call the function the database names that way, which MySQL, H2 and SQL Server do spell
     * YEAR, MONTH and DAY, whereas PostgreSQL and Oracle only know EXTRACT. See
     * {@link #supportsDatePart()}.
     */
    @Override
    public Expression<Integer> extract(CriteriaBuilder builder, Expression<?> expression,
            DatePart part) {
        return builder.function(part.name(), Integer.class, expression);
    }

    /**
     * The Criteria API names no cast of its own before Jakarta Persistence 3.2, so the function is
     * asked for by the name every provider knows it by, and each of them writes the cast its
     * database spells.
     */
    @Override
    public Expression<String> asText(CriteriaBuilder builder, Expression<?> expression) {
        return builder.function("str", String.class, expression);
    }

}
