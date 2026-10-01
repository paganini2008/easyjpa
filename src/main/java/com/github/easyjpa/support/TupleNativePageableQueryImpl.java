
package com.github.easyjpa.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.data.jpa.repository.query.QueryUtils;
import com.github.easyjpa.page.PageableQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;

/**
 *
 * Pagination of a native sql query, mapped row by row with nothing but the Criteria API. Selecting
 * into a Tuple is what gives the column labels away, which is what a RowMapper is handed.
 *
 * @Description: TupleNativePageableQueryImpl
 * @Author: Fred Feng
 * @Date: 17/08/2026
 * @Version 1.0.0
 */
public class TupleNativePageableQueryImpl<T> implements PageableQuery<T> {

    public TupleNativePageableQueryImpl(String sql, Object[] arguments, EntityManager em,
            RowMapper<T> rowMapper) {
        this.sql = sql;
        this.arguments = arguments;
        this.em = em;
        this.rowMapper = rowMapper;
    }

    private final String sql;
    private final Object[] arguments;
    private final EntityManager em;
    private final RowMapper<T> rowMapper;

    @Override
    public List<T> list(int maxResults, long firstResult) {
        Query query = setArguments(em.createNativeQuery(sql, Tuple.class));
        if (firstResult >= 0) {
            query.setFirstResult((int) firstResult);
        }
        if (maxResults > 0) {
            query.setMaxResults(maxResults);
        }
        List<T> results = new ArrayList<T>();
        int index = 0;
        for (Object each : query.getResultList()) {
            results.add(rowMapper.mapRow(index++, toMap((Tuple) each)));
        }
        return results;
    }

    @Override
    public long rowCount() {
        Object result = setArguments(em.createNativeQuery(getCountQuerySqlString(sql)))
                .getSingleResult();
        return result instanceof Number ? ((Number) result).longValue() : 0L;
    }

    // The database decides how a column label is cased, so a RowMapper should not have to care
    private Map<String, Object> toMap(Tuple tuple) {
        Map<String, Object> data = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        Map<String, Object> ordered = new LinkedHashMap<String, Object>();
        for (TupleElement<?> element : tuple.getElements()) {
            ordered.put(element.getAlias(), tuple.get(element));
        }
        data.putAll(ordered);
        return data;
    }

    private Query setArguments(Query query) {
        if (arguments != null && arguments.length > 0) {
            int index = 1;
            for (Object argument : arguments) {
                query.setParameter(index++, argument);
            }
        }
        return query;
    }

    protected String getCountQuerySqlString(String sql) {
        return String.format(QueryUtils.COUNT_QUERY_STRING, "1", "(" + sql + ")");
    }

}
