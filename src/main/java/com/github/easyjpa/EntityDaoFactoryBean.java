
package com.github.easyjpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import jakarta.persistence.EntityManager;

/**
 * 
 * Make EntityDaoSupport the repository implementation of Spring Data.
 * 
 * @Description: EntityDaoFactoryBean
 * @Author: Fred Feng
 * @Date: 18/10/2024
 * @Version 1.0.0
 */
@SuppressWarnings("all")
public abstract class EntityDaoFactoryBean<R extends JpaRepository<E, ID>, E, ID>
        extends JpaRepositoryFactoryBean<R, E, ID> {

    public EntityDaoFactoryBean(Class<R> repositoryInterface) {
        super(repositoryInterface);
    }

    /** The provider this dao is built upon, which every subclass names. */
    protected abstract JpaProvider getProvider();

    @Override
    protected RepositoryFactorySupport createRepositoryFactory(EntityManager em) {
        // Whoever is named here is the one the whole library talks to
        JpaProviders.setProvider(getProvider());
        return new ProviderDaoFactory<E, ID>(em);
    }

    private static class ProviderDaoFactory<E, ID> extends JpaRepositoryFactory {

        public ProviderDaoFactory(EntityManager em) {
            super(em);
        }

        @Override
        protected SimpleJpaRepository<E, ID> getTargetRepository(RepositoryInformation information,
                EntityManager entityManager) {
            return JpaProviders.getProvider()
                    .createDaoSupport((Class<E>) information.getDomainType(), entityManager);
        }

        @Override
        protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
            return JpaProviders.getProvider().getDaoSupportClass();
        }
    }

}
