
package com.github.easyjpa;

import java.util.List;
import com.github.easyjpa.page.PageableQuery;
import jakarta.persistence.criteria.CriteriaQuery;

/**
 *
 * Result set of a grouping pagination.
 * 
 * @Description: JpaGroupPageResultSetImpl
 * @Author: Fred Feng
 * @Date: 18/10/2024
 * @Version 1.0.0
 */
public class JpaGroupPageResultSetImpl<T> implements JpaPageResultSet<T> {

    private final Model<?> model;
    private final CriteriaQuery<T> query;
    private final JpaPageCount<?> counter;
    private final JpaCustomQuery<?> customQuery;

    JpaGroupPageResultSetImpl(Model<?> model, CriteriaQuery<T> query, JpaPageCount<?> counter,
            JpaCustomQuery<?> customQuery) {
        this.model = model;
        this.query = query;
        this.counter = counter;
        this.customQuery = customQuery;
    }

    @Override
    public List<T> list(int maxResults, long firstResult) {
        return customQuery.getResultList(builder -> query, maxResults, firstResult);
    }

    @Override
    public long rowCount() {
        return counter.rowCount(customQuery);
    }

    @Override
    public <R> PageableQuery<R> setTransformer(Transformer<T, R> transformer) {
        return new JpaGroupPageableQueryImpl<T, R>(model, query, counter, customQuery, transformer);
    }

}
