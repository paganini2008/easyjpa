package com.github.easyjpa.support;

import java.beans.PropertyDescriptor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.MutablePropertyValues;

/**
 *
 * Map a native sql row to a bean by matching the column labels to the bean properties.
 *
 * <p>
 * The matching ignores the case and the underscores, so a column labeled <code>PRODUCT_ID</code>,
 * <code>product_id</code> or <code>productId</code> all go to the property
 * <code>productId</code>. A column matching no property is skipped rather than rejected.
 * </p>
 *
 * @Description: BeanPropertyRowMapper
 * @Author: Fred Feng
 * @Date: 20/10/2024
 * @Version 1.0.0
 */
public class BeanPropertyRowMapper<T> implements RowMapper<T> {

    public BeanPropertyRowMapper(Class<T> resultClass, String... includedProperties) {
        this.resultClass = resultClass;
        this.includedPropertyNames = includedProperties != null && includedProperties.length > 0
                ? Set.of(includedProperties)
                : Collections.emptySet();
    }

    private final Class<T> resultClass;
    private final Set<String> includedPropertyNames;

    @Override
    public T mapRow(int index, Map<String, Object> data) {
        BeanWrapper beanWrapper = new BeanWrapperImpl(resultClass);
        Map<String, String> propertyNames = getPropertyNames(beanWrapper);
        MutablePropertyValues propertyValues = new MutablePropertyValues();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String propertyName = propertyNames.get(normalize(entry.getKey()));
            if (propertyName == null) {
                continue;
            }
            if (includedPropertyNames.isEmpty() || includedPropertyNames.contains(propertyName)) {
                propertyValues.add(propertyName, entry.getValue());
            }
        }
        beanWrapper.setPropertyValues(propertyValues, true);
        return resultClass.cast(beanWrapper.getWrappedInstance());
    }

    private Map<String, String> getPropertyNames(BeanWrapper beanWrapper) {
        Map<String, String> propertyNames = new HashMap<String, String>();
        for (PropertyDescriptor pd : beanWrapper.getPropertyDescriptors()) {
            if (pd.getWriteMethod() != null) {
                propertyNames.put(normalize(pd.getName()), pd.getName());
            }
        }
        return propertyNames;
    }

    private static String normalize(String name) {
        return name.replace("_", "").toLowerCase();
    }

}
