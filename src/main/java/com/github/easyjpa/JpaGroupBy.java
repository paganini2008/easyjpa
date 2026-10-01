
package com.github.easyjpa;

import jakarta.persistence.criteria.CriteriaQuery;

/**
 * 
 * The group by clause of a query.
 * 
 * @Description: JpaGroupBy
 * @Author: Fred Feng
 * @Date: 18/10/2024
 * @Version 1.0.0
 */
public interface JpaGroupBy<E, T> {

    JpaGroupBy<E, T> having(Filter filter);

    JpaQueryResultSet<T> select(ColumnList columnList);

    JpaGroupBy<E, T> sort(JpaSort... sorts);

    CriteriaQuery<T> query();

    Model<E> model();

}
