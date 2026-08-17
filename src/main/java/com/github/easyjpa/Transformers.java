
package com.github.easyjpa;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.LinkedCaseInsensitiveMap;
import jakarta.persistence.criteria.Selection;

/**
 * 
 * Transformer utilities, which transform a query result into a map, a list or a bean.
 * 
 * @Description: Transformers
 * @Author: Fred Feng
 * @Date: 18/10/2024
 * @Version 1.0.0
 */
public abstract class Transformers {

    public static <T> Transformer<T, T> noop() {
        return new NoopTransformer<T>();
    }

    public static <T> Transformer<T, Map<String, Object>> asMap() {
        return new MapTransformer<T>(false);
    }

    public static <T> Transformer<T, Map<String, Object>> asCaseInsensitiveMap() {
        return new MapTransformer<T>(true);
    }

    public static <T> Transformer<T, List<Object>> asList() {
        return new ListTransformer<T>();
    }

    public static <T, R> Transformer<T, R> asBean(Class<R> resultClass) {
        return asBean(resultClass, null);
    }

    public static <T, R> Transformer<T, R> asBean(Class<R> resultClass,
            TransformerPostHandler<T, R> postHandler) {
        return asBean(resultClass, null, postHandler);
    }

    public static <T, R> Transformer<T, R> asBean(Class<R> resultClass, String[] includedProperties,
            TransformerPostHandler<T, R> postHandler) {
        return new BeanPropertyTransformer<T, R>(resultClass, includedProperties, postHandler);
    }

    /**
     * 
     * @Description: NoopTransformer
     * @Author: Fred Feng
     * @Date: 18/10/2024
     * @Version 1.0.0
     */
    public static class NoopTransformer<T> implements Transformer<T, T> {

        NoopTransformer() {}

        @Override
        public T transfer(Model<?> model, T original) {
            return original;
        }

        @Override
        public T transfer(Model<?> model, List<Selection<?>> selections, T original) {
            return original;
        }

    }

    /**
     * 
     * @Description: MapTransformer
     * @Author: Fred Feng
     * @Date: 18/10/2024
     * @Version 1.0.0
     */
    public static class MapTransformer<T> extends AbstractTransformer<T, Map<String, Object>> {

        MapTransformer(boolean caseInsensitive) {
            this.caseInsensitive = caseInsensitive;
        }

        private final boolean caseInsensitive;

        @Override
        protected Map<String, Object> createObject(Model<?> model, int selectionSize, T original) {
            return caseInsensitive ? new LinkedCaseInsensitiveMap<>()
                    : new LinkedHashMap<>(selectionSize);
        }

        @Override
        protected void writeValue(Model<?> model, String attributeName, Class<?> attributeType,
                Object attributeValue, Map<String, Object> data) {
            data.put(attributeName, attributeValue);
        }

    }

    /**
     * 
     * @Description: ListTransformer
     * @Author: Fred Feng
     * @Date: 21/10/2024
     * @Version 1.0.0
     */
    public static class ListTransformer<T> extends AbstractTransformer<T, List<Object>> {

        ListTransformer() {}

        @Override
        protected List<Object> createObject(Model<?> model, int selectionSize, T original) {
            return new ArrayList<Object>(selectionSize);
        }

        @Override
        protected void writeValue(Model<?> model, String attributeName, Class<?> attributeType,
                Object attributeValue, List<Object> dataList) {
            dataList.add(attributeValue);
        }

    }

}
