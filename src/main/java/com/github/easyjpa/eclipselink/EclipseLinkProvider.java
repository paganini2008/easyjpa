
package com.github.easyjpa.eclipselink;

import com.github.easyjpa.EntityDaoSupport;
import com.github.easyjpa.StandardProvider;
import jakarta.persistence.EntityManager;

/**
 *
 * EclipseLink reaches no further than the Criteria API does on the points this library cares
 * about: it has no subquery in the from clause, so a grouping pagination is counted by a
 * count(distinct ...) and a derived table can not be joined, and it renders no date part of its
 * own. Everything else is what {@link StandardProvider} already does.
 *
 * @Description: EclipseLinkProvider
 * @Author: Fred Feng
 * @Date: 17/08/2026
 * @Version 1.0.0
 */
public class EclipseLinkProvider extends StandardProvider {

    @Override
    public String getName() {
        return "EclipseLink";
    }

    /**
     * EclipseLink runs itself into an endless recursion while copying such an expression, so the
     * groups of several keys are counted one by one here.
     */
    @Override
    public boolean supportsConcatenatedGroupKey() {
        return false;
    }

    @Override
    public boolean supportsRightJoin() {
        return false;
    }

    @Override
    public boolean supportsComparisonAsSelection() {
        return false;
    }

    @Override
    public boolean supportsOrdinalSort() {
        return false;
    }

    @Override
    public boolean supportsPassThroughFunction() {
        return false;
    }

    @Override
    public boolean supportsSubQueryAsExpression() {
        return false;
    }

    @Override
    public boolean supportsSubQueryAsSelection() {
        return false;
    }

    /** A partly filled entity would poison the cache of EclipseLink, so it refuses to build one. */
    @Override
    public boolean supportsPartialEntity() {
        return false;
    }

    /** EclipseLink writes the columns into a bean through a matching constructor alone. */
    @Override
    public boolean supportsBeanProjection() {
        return false;
    }

    @Override
    public <E, ID> EntityDaoSupport<E, ID> createDaoSupport(Class<E> entityClass,
            EntityManager em) {
        return new EclipseLinkDaoSupport<E, ID>(entityClass, em);
    }

    @Override
    public Class<?> getDaoSupportClass() {
        return EclipseLinkDaoSupport.class;
    }

}
