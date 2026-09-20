
package com.github.easyjpa.eclipselink;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.eclipse.persistence.config.QueryHints;
import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.config.ResultType;
import org.springframework.data.jpa.repository.query.QueryUtils;
import com.github.easyjpa.page.PageableQuery;
import com.github.easyjpa.support.RowMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

/**
 *
 * Pagination of a native sql query upon EclipseLink, which hands every row over as a map of its
 * own once it is asked to.
 *
 * @Description: EclipseLinkNativePageableQueryImpl
 * @Author: Fred Feng
 * @Date: 17/08/2026
 * @Version 1.0.0
 */
public class EclipseLinkNativePageableQueryImpl<T> implements PageableQuery<T> {

    public EclipseLinkNativePageableQueryImpl(String sql, Object[] arguments, EntityManager em,
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
        Query query = setArguments(em.createNativeQuery(sql))
                .setHint(QueryHints.RESULT_TYPE, ResultType.Map);
        if (firstResult >= 0) {
            query.setFirstResult((int) firstResult);
        }
        if (maxResults > 0) {
            query.setMaxResults(maxResults);
        }
        List<T> results = new ArrayList<T>();
        int index = 0;
        for (Object each : query.getResultList()) {
            results.add(rowMapper.mapRow(index++, toMap((Map<?, ?>) each)));
        }
        return results;
    }

    @Override
    public long rowCount() {
        Object result =
                setArguments(em.createNativeQuery(getCountQuerySqlString(sql))).getSingleResult();
        return result instanceof Number ? ((Number) result).longValue() : 0L;
    }

    /**
     * EclipseLink keys every row by the database field itself, and the database decides how a
     * column label is cased, so a RowMapper is handed the plain names matched case insensitively.
     */
    private Map<String, Object> toMap(Map<?, ?> data) {
        Map<String, Object> copy = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        data.forEach((key, value) -> copy.put(
                key instanceof DatabaseField ? ((DatabaseField) key).getName() : String.valueOf(key),
                value));
        return copy;
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
