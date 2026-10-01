
package com.github.easyjpa;

import com.github.easyjpa.page.PageableQuery;
import com.github.easyjpa.support.BeanPropertyRowMapper;
import com.github.easyjpa.support.RowMapper;
import com.github.easyjpa.support.TupleNativePageableQueryImpl;
import jakarta.persistence.EntityManager;

/**
 *
 * The dao backed by nothing but the Criteria API, which is what a provider of its own brings
 * nothing better to.
 *
 * @Description: StandardDaoSupport
 * @Author: Fred Feng
 * @Date: 17/08/2026
 * @Version 1.0.0
 */
public class StandardDaoSupport<E, ID> extends EntityDaoSupport<E, ID> implements EntityDao<E, ID> {

    public StandardDaoSupport(Class<E> entityClass, EntityManager em) {
        super(entityClass, em);
    }

    @Override
    public <T> PageableQuery<T> query(String sql, Object[] arguments, Class<T> resultClass) {
        return query(sql, arguments, new BeanPropertyRowMapper<T>(resultClass));
    }

    @Override
    public <T> PageableQuery<T> query(String sql, Object[] arguments, RowMapper<T> rowMapper) {
        return new TupleNativePageableQueryImpl<T>(sql, arguments, em, rowMapper);
    }

}
