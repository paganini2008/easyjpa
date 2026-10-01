
package com.github.easyjpa.eclipselink;

import org.springframework.data.jpa.repository.JpaRepository;
import com.github.easyjpa.EntityDaoFactoryBean;
import com.github.easyjpa.JpaProvider;

/**
 * 
 * Build the daos upon EclipseLink, where the derived tables and the date parts are out of reach.
 * 
 * @Description: EclipseLinkEntityDaoFactoryBean
 * @Author: Fred Feng
 * @Date: 17/08/2026
 * @Version 1.0.0
 */
public class EclipseLinkEntityDaoFactoryBean<R extends JpaRepository<E, ID>, E, ID>
        extends EntityDaoFactoryBean<R, E, ID> {

    public EclipseLinkEntityDaoFactoryBean(Class<R> repositoryInterface) {
        super(repositoryInterface);
    }

    @Override
    protected JpaProvider getProvider() {
        return new EclipseLinkProvider();
    }

}
