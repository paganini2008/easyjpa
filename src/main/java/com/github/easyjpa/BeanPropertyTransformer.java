
package com.github.easyjpa;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

/**
 * 
 * Transform a query result into a bean by matching the selection aliases to the bean properties.
 * 
 * @Description: BeanPropertyTransformer
 * @Author: Fred Feng
 * @Date: 18/10/2024
 * @Version 1.0.0
 */
public class BeanPropertyTransformer<T, R> extends AbstractTransformer<T, R> {

    private static final Logger log = LoggerFactory.getLogger(BeanPropertyTransformer.class);

    public BeanPropertyTransformer(Class<R> resultClass, String[] includedProperties,
            TransformerPostHandler<T, R> postHandler) {
        this.resultClass = resultClass;
        this.includedProperties = includedProperties;
        this.postHandler = postHandler;
    }

    private final Class<R> resultClass;
    private final String[] includedProperties;
    private final TransformerPostHandler<T, R> postHandler;

    @Override
    protected void writeValue(Model<?> model, String attributeName, Class<?> attributeType,
            Object attributeValue, R destination) {
        if (includedProperties == null || ArrayUtils.contains(includedProperties, attributeName)) {
            try {
                PropertyUtils.setProperty(destination, attributeName, attributeValue);
            } catch (Exception e) {
                if (log.isWarnEnabled()) {
                    log.warn("Failed to assign value to attribute name '{}' due to reason: {}",
                            attributeName, e.getMessage());
                }
            }
        }
    }

    @Override
    protected R createObject(Model<?> model, int selectionSize, T original) {
        return BeanUtils.instantiateClass(resultClass);
    }

    @Override
    protected final void afterTransformation(Model<?> model, T original, R destination) {
        if (postHandler != null) {
            postHandler.handleAfterTransformation(model, original, destination);
        }
    }

}
