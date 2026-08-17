
package com.github.easyjpa;

import java.util.ArrayList;
import java.util.List;
import com.github.easyjpa.page.PageableQuery;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Selection;

/**
 * 
 * Transformed result of a pagination.
 * 
 * @Description: JpaPageableQueryImpl
 * @Author: Fred Feng
 * @Date: 18/10/2024
 * @Version 1.0.0
 */
public class JpaPageableQueryImpl<T, R> implements PageableQuery<R> {

    private final Model<?> model;
    private final CriteriaQuery<T> query;
    private final JpaPageCount<?> counter;
    private final JpaCustomQuery<?> customQuery;
    private final Transformer<T, R> transformer;

    JpaPageableQueryImpl(Model<?> model, CriteriaQuery<T> query, JpaPageCount<?> counter,
            JpaCustomQuery<?> customQuery, Transformer<T, R> transformer) {
        this.model = model;
        this.query = query;
        this.counter = counter;
        this.customQuery = customQuery;
        this.transformer = transformer;
    }

    @Override
    public long rowCount() {
        return counter.rowCount(customQuery);
    }

    @Override
    public List<R> list(int maxResults, long firstResult) {
        List<R> results = new ArrayList<R>();
        List<T> list = customQuery.getResultList(builder -> query, maxResults, firstResult);
        if (query.getResultType() == model.getRootType()) {
            for (T original : list) {
                R data = transformer.transfer(model, original);
                results.add(data);
            }
        } else {
            List<Selection<?>> selections = query.getSelection().getCompoundSelectionItems();
            for (T original : list) {
                R data = transformer.transfer(model, selections, original);
                results.add(data);
            }
        }
        return results;
    }

}
