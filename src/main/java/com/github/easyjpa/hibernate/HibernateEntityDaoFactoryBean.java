
package com.github.easyjpa.hibernate;

import org.springframework.data.jpa.repository.JpaRepository;
import com.github.easyjpa.EntityDaoFactoryBean;
import com.github.easyjpa.JpaProvider;

/**
 * 
 * Build the daos upon Hibernate, which is the default.
 * 
 * @Description: HibernateEntityDaoFactoryBean
 * @Author: Fred Feng
 * @Date: 17/08/2026
 * @Version 1.0.0
 */
public class HibernateEntityDaoFactoryBean<R extends JpaRepository<E, ID>, E, ID>
        extends EntityDaoFactoryBean<R, E, ID> {

    public HibernateEntityDaoFactoryBean(Class<R> repositoryInterface) {
        super(repositoryInterface);
    }

    @Override
    protected JpaProvider getProvider() {
        return new HibernateProvider();
    }

}
