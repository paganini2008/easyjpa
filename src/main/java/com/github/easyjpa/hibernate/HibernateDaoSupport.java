
package com.github.easyjpa.hibernate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.sql.internal.NativeQueryImpl;
import org.hibernate.transform.Transformers;
import com.github.easyjpa.EntityDao;
import com.github.easyjpa.EntityDaoSupport;
import com.github.easyjpa.page.PageableQuery;
import com.github.easyjpa.support.BeanPropertyRowMapper;
import com.github.easyjpa.support.RowMapper;
import jakarta.persistence.EntityManager;

/**
 * 
 * @Description: HibernateDaoSupport
 * @Author: Fred Feng
 * @Date: 20/10/2024
 * @Version 1.0.0
 */
public class HibernateDaoSupport<E, ID> extends EntityDaoSupport<E, ID>
        implements EntityDao<E, ID> {

    public HibernateDaoSupport(Class<E> entityClass, EntityManager em) {
        super(entityClass, em);
    }

    @Override
    public <T> PageableQuery<T> query(String sql, Object[] arguments, Class<T> resultClass) {
        return new HibernateNativePageableQueryImpl<T>(sql, arguments, em,
                new BeanPropertyQueryResultSetExtractor<T>(resultClass));
    }

    @Override
    public <T> PageableQuery<T> query(String sql, Object[] arguments, RowMapper<T> rowMapper) {
        return new HibernateNativePageableQueryImpl<T>(sql, arguments, em,
                new MappedQueryResultSetExtractor<T>(rowMapper));
    }

    @SuppressWarnings("unchecked")
    private static class MappedQueryResultSetExtractor<T> implements QueryResultSetExtractor<T> {

        private final RowMapper<T> rowMapper;

        MappedQueryResultSetExtractor(RowMapper<T> rowMapper) {
            this.rowMapper = rowMapper;
        }

        @Override
        public List<T> extractData(Session session, NativeQuery<?> query) {
            List<T> results = new ArrayList<T>();
            query.unwrap(NativeQueryImpl.class)
                    .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) query.getResultList();
            int index = 0;
            for (Map<String, Object> data : dataList) {
                T mappedResult = rowMapper.mapRow(index++, caseInsensitive(data));
                results.add(mappedResult);
            }
            return results;
        }

        // The database decides how a column label is cased, so a RowMapper should not have to care.
        private Map<String, Object> caseInsensitive(Map<String, Object> data) {
            Map<String, Object> copy = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
            copy.putAll(data);
            return copy;
        }

    }

    private static class BeanPropertyQueryResultSetExtractor<T>
            extends MappedQueryResultSetExtractor<T> {

        BeanPropertyQueryResultSetExtractor(Class<T> resultClass) {
            super(new BeanPropertyRowMapper<T>(resultClass));
        }

    }

}
