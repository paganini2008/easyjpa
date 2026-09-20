
package com.github.easyjpa;

import java.util.List;
import jakarta.persistence.criteria.CriteriaQuery;

/**
 * 
 * Default JpaQueryResultSet implementation.
 * 
 * @Description: JpaQueryResultSetImpl
 * @Author: Fred Feng
 * @Date: 20/10/2024
 * @Version 1.0.0
 */
public class JpaQueryResultSetImpl<T> implements JpaQueryResultSet<T> {

    private final Model<?> model;
    private final CriteriaQuery<T> query;
    private final JpaCustomQuery<?> customQuery;

    JpaQueryResultSetImpl(Model<?> model, CriteriaQuery<T> query, JpaCustomQuery<?> customQuery) {
        this.model = model;
        this.query = query;
        this.customQuery = customQuery;
    }

    @Override
    public T one() {
        return customQuery.getSingleResult(builder -> query);
    }

    @Override
    public List<T> list(int maxResults, long firstResult) {
        if (maxResults == -1 && firstResult == 0) {
            return customQuery.getResultList(builder -> query);
        }
        return customQuery.getResultList(builder -> query, maxResults, firstResult);
    }

    @Override
    public <R> ListableQuery<R> setTransformer(Transformer<T, R> transformer) {
        return new JpaListableQueryImpl<T, R>(model, query, customQuery, transformer);
    }
}
