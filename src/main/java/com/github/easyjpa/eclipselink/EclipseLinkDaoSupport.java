
package com.github.easyjpa.eclipselink;

import com.github.easyjpa.EntityDao;
import com.github.easyjpa.EntityDaoSupport;
import com.github.easyjpa.page.PageableQuery;
import com.github.easyjpa.support.BeanPropertyRowMapper;
import com.github.easyjpa.support.RowMapper;
import jakarta.persistence.EntityManager;

/**
 *
 * The dao upon EclipseLink, which differs from the standard one only in how a native sql result is
 * handed over.
 *
 * @Description: EclipseLinkDaoSupport
 * @Author: Fred Feng
 * @Date: 17/08/2026
 * @Version 1.0.0
 */
public class EclipseLinkDaoSupport<E, ID> extends EntityDaoSupport<E, ID>
        implements EntityDao<E, ID> {

    public EclipseLinkDaoSupport(Class<E> entityClass, EntityManager em) {
        super(entityClass, em);
    }

    @Override
    public <T> PageableQuery<T> query(String sql, Object[] arguments, Class<T> resultClass) {
        return query(sql, arguments, new BeanPropertyRowMapper<T>(resultClass));
    }

    @Override
    public <T> PageableQuery<T> query(String sql, Object[] arguments, RowMapper<T> rowMapper) {
        return new EclipseLinkNativePageableQueryImpl<T>(sql, arguments, em, rowMapper);
    }

}
