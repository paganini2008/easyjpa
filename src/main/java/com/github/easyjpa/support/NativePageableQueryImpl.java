
package com.github.easyjpa.support;

import java.util.List;
import org.springframework.data.jpa.repository.query.QueryUtils;
import com.github.easyjpa.page.PageableQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

/**
 * 
 * Pagination of a native sql query.
 * 
 * @Description: NativePageableQueryImpl
 * @Author: Fred Feng
 * @Date: 21/08/2021
 * @Version 1.0.0
 */
@SuppressWarnings("all")
public class NativePageableQueryImpl<E> implements PageableQuery<E> {

    public NativePageableQueryImpl(String sql, Object[] arguments, Class<E> entityClass,
            EntityManager em) {
        this.sql = sql;
        this.arguments = arguments;
        this.em = em;
        this.entityClass = entityClass;
    }

    private final String sql;
    private final Object[] arguments;
    private final EntityManager em;
    private final Class<E> entityClass;

    public List<E> list(int maxResults, long firstResult) {
        Query query = entityClass != null ? em.createNativeQuery(sql, entityClass)
                : em.createNativeQuery(sql);
        if (arguments != null && arguments.length > 0) {
            int index = 1;
            for (Object arg : arguments) {
                query.setParameter(index++, arg);
            }
        }
        if (firstResult >= 0) {
            query.setFirstResult((int) firstResult);
        }
        if (maxResults > 0) {
            query.setMaxResults((int) maxResults);
        }
        return query.getResultList();
    }

    public long rowCount() {
        Query query = em.createNativeQuery(getCountQuerySqlString(sql));
        if (arguments != null && arguments.length > 0) {
            int index = 1;
            for (Object arg : arguments) {
                query.setParameter(index++, arg);
            }
        }
        Object result = query.getSingleResult();
        return result instanceof Number ? ((Number) result).longValue() : 0L;
    }

    protected String getCountQuerySqlString(String sql) {
        return String.format(QueryUtils.COUNT_QUERY_STRING, "1", "(" + sql + ")");
    }

}
