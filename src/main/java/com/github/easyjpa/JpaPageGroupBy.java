
package com.github.easyjpa;

import jakarta.persistence.criteria.CriteriaQuery;

/**
 * 
 * The group by clause of a pagination.
 * 
 * @Description: JpaPageGroupBy
 * @Author: Fred Feng
 * @Date: 18/10/2024
 * @Version 1.0.0
 */
public interface JpaPageGroupBy<E, T> {

    JpaPageGroupBy<E, T> having(Filter expression);

    JpaPageResultSet<T> select(ColumnList columnList);

    JpaPageGroupBy<E, T> sort(JpaSort... sorts);

    CriteriaQuery<T> query();

    Model<E> model();

}
