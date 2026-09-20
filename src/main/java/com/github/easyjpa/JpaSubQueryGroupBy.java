
package com.github.easyjpa;

/**
 * 
 * The group by clause of a subquery.
 * 
 * @Description: JpaSubQueryGroupBy
 * @Author: Fred Feng
 * @Date: 18/10/2024
 * @Version 1.0.0
 */
public interface JpaSubQueryGroupBy<E, T> extends SubQueryBuilder<T> {

    JpaSubQueryGroupBy<E, T> having(Filter filter);

    default JpaSubQueryGroupBy<E, T> select(String attributeName) {
        return select(null, attributeName);
    }

    JpaSubQueryGroupBy<E, T> select(String alias, String attributeName);

    JpaSubQueryGroupBy<E, T> select(Field<T> field);

    /**
     * Select several columns, which is what a derived table needs. Every column has to be aliased,
     * since the aliases become the column names of that table.
     */
    JpaSubQueryGroupBy<E, T> select(ColumnList columnList);

    default JpaSubQueryGroupBy<E, T> select(SerializableFunction<E, T> function) {
        return select(Property.forName(function));
    }

}
