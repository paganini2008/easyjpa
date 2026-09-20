
package com.github.easyjpa;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 
 * Build the daos upon the Criteria API alone, whatever provider is running underneath. See
 * {@link StandardProvider} for what that leaves out.
 * 
 * @Description: StandardEntityDaoFactoryBean
 * @Author: Fred Feng
 * @Date: 17/08/2026
 * @Version 1.0.0
 */
public class StandardEntityDaoFactoryBean<R extends JpaRepository<E, ID>, E, ID>
        extends EntityDaoFactoryBean<R, E, ID> {

    public StandardEntityDaoFactoryBean(Class<R> repositoryInterface) {
        super(repositoryInterface);
    }

    @Override
    protected JpaProvider getProvider() {
        return new StandardProvider();
    }

}
