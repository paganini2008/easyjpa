
package com.github.easyjpa;

import java.util.List;
import com.github.easyjpa.page.PageableQuery;
import jakarta.persistence.criteria.CriteriaQuery;

/**
 * 
 * Result set of a non grouping pagination.
 * 
 * @Description: JpaPageResultSetImpl
 * @Author: Fred Feng
 * @Date: 20/10/2024
 * @Version 1.0.0
 */
public class JpaPageResultSetImpl<T> implements JpaPageResultSet<T> {

    private final Model<?> model;
    private final CriteriaQuery<T> query;
    private final JpaPageCount<?> counter;
    private final JpaCustomQuery<?> customQuery;

    JpaPageResultSetImpl(Model<?> model, CriteriaQuery<T> query, JpaPageCount<?> counter,
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
    public long rowCount() throws Exception {
        return counter.rowCount(customQuery);
    }

    @Override
    public <R> PageableQuery<R> setTransformer(Transformer<T, R> transformer) {
        return new JpaPageableQueryImpl<T, R>(model, query, counter, customQuery, transformer);
    }

}
