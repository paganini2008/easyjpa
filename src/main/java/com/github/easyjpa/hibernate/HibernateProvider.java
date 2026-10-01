
package com.github.easyjpa.hibernate;

import java.time.temporal.TemporalAccessor;
import java.util.List;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaSubQuery;
import com.github.easyjpa.EntityDaoSupport;
import com.github.easyjpa.Filter;
import com.github.easyjpa.JpaPageCount;
import com.github.easyjpa.JpaProvider;
import com.github.easyjpa.Model;
import com.github.easyjpa.RootModel;
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
 * What Hibernate reaches beyond the Criteria API: a subquery may occur in the from clause, either
 * as the derived table a pagination counts by or as one more table to join, a subquery may select
 * several columns, and the parts of a date are rendered the way every database spells them.
 *
 * @Description: HibernateProvider
 * @Author: Fred Feng
 * @Date: 17/08/2026
 * @Version 1.0.0
 */
public class HibernateProvider implements JpaProvider {

    @Override
    public String getName() {
        return "Hibernate";
    }

    @Override
    public boolean supportsDerivedTable() {
        return true;
    }

    @Override
    public boolean supportsDatePart() {
        return true;
    }

    @Override
    public <E, ID> EntityDaoSupport<E, ID> createDaoSupport(Class<E> entityClass,
            EntityManager em) {
        return new HibernateDaoSupport<E, ID>(entityClass, em);
    }

    @Override
    public Class<?> getDaoSupportClass() {
        return HibernateDaoSupport.class;
    }

    @Override
    public <E> JpaPageCount<E> createPageCount(Class<E> entityClass, String alias,
            EntityManager em) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<E> root = subquery.from(entityClass);
        return new DerivedTablePageCount<E>(new RootModel<E>(root, alias, em.getMetamodel()),
                query, subquery, builder);
    }

    @Override
    public <X> Model<X> joinSubQuery(Model<X> model, Subquery<?> subquery, String alias,
            JoinType joinType, Filter on, CriteriaBuilder builder) {
        return DerivedModel.join(model, subquery, alias, joinType, on, builder);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void multiselect(Subquery<?> subquery, List<Selection<?>> selections) {
        ((JpaSubQuery<Object>) subquery).multiselect(selections);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Expression<Integer> extract(CriteriaBuilder builder, Expression<?> expression,
            DatePart part) {
        HibernateCriteriaBuilder criteriaBuilder = (HibernateCriteriaBuilder) builder;
        Expression<? extends TemporalAccessor> temporal =
                (Expression<? extends TemporalAccessor>) expression;
        switch (part) {
            case YEAR:
                return criteriaBuilder.year(temporal);
            case MONTH:
                return criteriaBuilder.month(temporal);
            default:
                return criteriaBuilder.day(temporal);
        }
    }

}
