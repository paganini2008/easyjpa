package com.github.easyjpa;

import java.util.List;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Root;

/**
 * 
 * @Description: JpaDaoSupport
 * @Author: Fred Feng
 * @Date: 11/10/2024
 * @Version 1.0.0
 */
public class JpaDaoSupport<E, ID> extends SimpleJpaRepository<E, ID>
        implements JpaCustomUpdate<E>, JpaCustomQuery<E> {

    public JpaDaoSupport(Class<E> entityClass, EntityManager em) {
        super(entityClass, em);
        this.em = em;
    }

    protected final EntityManager em;

    @Override
    public <T> T getSingleResult(JpaQueryCallback<T> callback) {
        try {
            CriteriaBuilder builder = em.getCriteriaBuilder();
            CriteriaQuery<T> query = callback.doInJpa(builder);
            TypedQuery<T> typedQuery = em.createQuery(query);
            return typedQuery.getSingleResult();
        } finally {
            TableAlias.clear();
        }
    }

    @Override
    public <T> List<T> getResultList(JpaQueryCallback<T> callback) {
        try {
            CriteriaBuilder builder = em.getCriteriaBuilder();
            CriteriaQuery<T> query = callback.doInJpa(builder);
            TypedQuery<T> typedQuery = em.createQuery(query);
            return typedQuery.getResultList();
        } finally {
            TableAlias.clear();
        }
    }

    @Override
    public <T> List<T> getResultList(JpaQueryCallback<T> callback, int maxResults,
            long firstResult) {
        try {
            CriteriaBuilder builder = em.getCriteriaBuilder();
            CriteriaQuery<T> query = callback.doInJpa(builder);
            TypedQuery<T> typedQuery = em.createQuery(query);
            if (firstResult >= 0) {
                typedQuery.setFirstResult((int) firstResult);
            }
            if (maxResults > 0) {
                typedQuery.setMaxResults(maxResults);
            }
            return typedQuery.getResultList();
        } finally {
            TableAlias.clear();
        }
    }

    @Override
    public int executeUpdate(JpaDeleteCallback<E> callback) {
        try {
            CriteriaBuilder builder = em.getCriteriaBuilder();
            CriteriaDelete<E> delete = callback.doInJpa(builder);
            Query query = em.createQuery(delete);
            return query.executeUpdate();
        } finally {
            TableAlias.clear();
        }
    }

    @Override
    public int executeUpdate(JpaUpdateCallback<E> callback) {
        try {
            CriteriaBuilder builder = em.getCriteriaBuilder();
            CriteriaUpdate<E> update = callback.doInJpa(builder);
            Query query = em.createQuery(update);
            return query.executeUpdate();
        } finally {
            TableAlias.clear();
        }
    }

    @Override
    public JpaUpdate<E> update(Class<E> entityClass) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaUpdate<E> update = builder.createCriteriaUpdate(entityClass);
        Root<E> root = update.from(entityClass);
        return new JpaUpdateImpl<E>(new RootModel<E>(root, Model.ROOT, em.getMetamodel()), update,
                builder, this);
    }

    @Override
    public JpaDelete<E> delete(Class<E> entityClass) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaDelete<E> delete = builder.createCriteriaDelete(entityClass);
        Root<E> root = delete.from(entityClass);
        return new JpaDeleteImpl<E>(new RootModel<E>(root, Model.ROOT, em.getMetamodel()), delete,
                builder, this);
    }

    public JpaQuery<E, Tuple> query(Class<E> entityClass, String alias) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = builder.createTupleQuery();
        Root<E> root = query.from(entityClass);
        return new JpaQueryImpl<E, Tuple>(new RootModel<E>(root, alias, em.getMetamodel()), query,
                builder, this);
    }

    public <T> JpaQuery<E, T> query(Class<E> entityClass, String alias, Class<T> resultClass) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<T> query = builder.createQuery(resultClass);
        Root<E> root = query.from(entityClass);
        return new JpaQueryImpl<E, T>(new RootModel<E>(root, alias, em.getMetamodel()), query,
                builder, this);
    }

    public <T> JpaPage<E, Tuple> page(Class<E> entityClass, String alias) {
        JpaQuery<E, Tuple> query = query(entityClass, alias);
        JpaQuery<E, Long> counter = query(entityClass, alias, Long.class);
        return new JpaPageImpl<E, Tuple>(query, counter, this);
    }

    public <T> JpaPage<E, T> page(Class<E> entityClass, String alias, Class<T> resultClass) {
        JpaQuery<E, T> query = query(entityClass, alias, resultClass);
        JpaQuery<E, Long> counter = query(entityClass, alias, Long.class);
        return new JpaPageImpl<E, T>(query, counter, this);
    }

}
