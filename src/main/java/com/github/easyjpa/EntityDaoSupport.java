package com.github.easyjpa;

import static com.github.easyjpa.Model.ROOT;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.github.easyjpa.page.PageableQuery;
import com.github.easyjpa.support.NativePageableQueryImpl;
import com.github.easyjpa.support.ResultSetExtractor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;

/**
 * 
 * Default EntityDao implementation.
 * 
 * @Description: EntityDaoSupport
 * @Author: Fred Feng
 * @Date: 18/10/2024
 * @Version 1.0.0
 */
public abstract class EntityDaoSupport<E, ID> extends JpaDaoSupport<E, ID>
        implements EntityDao<E, ID> {

    public EntityDaoSupport(Class<E> entityClass, EntityManager em) {
        super(entityClass, em);
        this.entityClass = entityClass;
    }

    protected final Class<E> entityClass;

    @Override
    public boolean exists(Filter filter) {
        return count(filter) > 0;
    }


    @Override
    public long count(Filter filter) {
        return count((root, query, builder) -> {
            return filter.toPredicate(Model.forRoot(root), builder);
        });
    }

    @Override
    public List<E> findAll(Filter filter) {
        return findAll((root, query, builder) -> {
            return filter.toPredicate(Model.forRoot(root), builder);
        });
    }

    @Override
    public List<E> findAll(Filter filter, Sort sort) {
        return findAll((root, query, builder) -> {
            return filter.toPredicate(Model.forRoot(root), builder);
        }, sort);
    }

    @Override
    public Page<E> findAll(Filter filter, Pageable pageable) {
        return findAll((root, query, builder) -> {
            return filter.toPredicate(Model.forRoot(root), builder);
        }, pageable);
    }

    @Override
    public Optional<E> findOne(Filter filter) {
        return findOne((root, query, builder) -> {
            return filter.toPredicate(Model.forRoot(root), builder);
        });
    }

    @Override
    public <T extends Number> T sum(String attributeName, Filter filter, Class<T> requiredType) {
        Property<T> property = Property.forName(null, attributeName, requiredType);
        return query(requiredType).filter(filter).one(Fields.sum(property));
    }

    @Override
    public Double avg(String attributeName, Filter filter) {
        Property<Double> property = Property.forName(null, attributeName);
        return query(Double.class).filter(filter).one(Fields.avg(property));
    }

    @Override
    public <T extends Comparable<T>> T min(String attributeName, Filter filter,
            Class<T> requiredType) {
        Property<T> property = Property.forName(null, attributeName, requiredType);
        return query(requiredType).filter(filter).one(Fields.min(property));
    }

    @Override
    public <T extends Comparable<T>> T max(String attributeName, Filter filter,
            Class<T> requiredType) {
        Property<T> property = Property.forName(null, attributeName, requiredType);
        return query(requiredType).filter(filter).one(Fields.max(property));
    }

    @Override
    public JpaDelete<E> delete() {
        return delete(entityClass);
    }

    @Override
    public JpaUpdate<E> update() {
        return update(entityClass);
    }

    @Override
    public JpaQuery<E, E> query() {
        return query(entityClass, ROOT, entityClass);
    }

    @Override
    public <T> JpaQuery<E, T> query(Class<T> resultClass) {
        return query(entityClass, ROOT, resultClass);
    }

    @Override
    public JpaQuery<E, Tuple> customQuery() {
        return query(entityClass, ROOT);
    }

    @Override
    public JpaPage<E, E> page() {
        return page(entityClass, ROOT, entityClass);
    }

    @Override
    public <T> JpaPage<E, T> page(Class<T> resultClass) {
        return page(entityClass, ROOT, resultClass);
    }

    @Override
    public JpaPage<E, Tuple> customPage() {
        return page(entityClass, ROOT);
    }

    @Override
    public PageableQuery<E> query(String sql, Object[] arguments) {
        return new NativePageableQueryImpl<E>(sql, arguments, entityClass, em);
    }

    @Override
    public <T> T getSingleResult(String sql, Object[] arguments, Class<T> requiredType) {
        return execute(sql, arguments, query -> {
            Object result = query.getSingleResult();
            try {
                return requiredType.cast(result);
            } catch (RuntimeException e) {
                return (T) ConvertUtils.convertValue(result, requiredType);
            }
        });
    }

    @Override
    public int executeUpdate(String sql, Object[] arguments) {
        return execute(sql, arguments, query -> {
            return query.executeUpdate();
        });
    }

    @Override
    public <T> T execute(String sql, Object[] arguments, ResultSetExtractor<T> extractor) {
        Query query = em.createNativeQuery(sql);
        if (arguments != null && arguments.length > 0) {
            int index = 1;
            for (Object arg : arguments) {
                query.setParameter(index++, arg);
            }
        }
        return extractor.extractData(query);
    }

    public Class<E> getEntityClass() {
        return entityClass;
    }

}
