package com.github.easyjpa;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.ReflectionUtils;

/**
 * 
 * Read and write the bean properties reflectively.
 * 
 * @Description: PropertyUtils
 * @Author: Fred Feng
 * @Date: 21/10/2024
 * @Version 1.0.0
 */
public abstract class PropertyUtils {

    private static final DefaultConversionService conversionService =
            new DefaultConversionService();

    public static Object getProperty(Object object, String attributeName) {
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(object);
        PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(targetClass, attributeName);
        try {
            if (pd != null && pd.getReadMethod() != null) {
                Method method = pd.getReadMethod();
                ReflectionUtils.makeAccessible(method);
                return method.invoke(object);
            } else {
                java.lang.reflect.Field field =
                        ReflectionUtils.findField(targetClass, attributeName);
                ReflectionUtils.makeAccessible(field);
                return field.get(object);
            }
        } catch (Exception e) {
            throw new PropertyAccessException(e.getMessage(), e);
        }
    }

    public static void setProperty(Object object, String attributeName, Object attributeValue)
            throws Exception {
        setProperty(object, attributeName, attributeValue, conversionService);
    }

    public static void setProperty(Object object, String attributeName, Object attributeValue,
            ConversionService conversionService) {
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(object);
        PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(targetClass, attributeName);
        try {
            if (pd != null && pd.getWriteMethod() != null) {
                Method method = pd.getWriteMethod();
                ReflectionUtils.makeAccessible(method);
                if (attributeValue != null && conversionService != null) {
                    attributeValue = convertValueWithTargetType(conversionService, attributeValue,
                            pd.getPropertyType());
                }
                method.invoke(object, attributeValue);
            } else {
                java.lang.reflect.Field field =
                        ReflectionUtils.findField(targetClass, attributeName);
                ReflectionUtils.makeAccessible(field);
                if (attributeValue != null && conversionService != null) {
                    attributeValue = convertValueWithTargetType(conversionService, attributeValue,
                            field.getType());
                }
                field.set(object, attributeValue);
            }
        } catch (Exception e) {
            throw new PropertyAccessException(e.getMessage(), e);
        }
    }

    private static Object convertValueWithTargetType(ConversionService conversionService,
            Object attributeValue, Class<?> targetType) {
        try {
            return targetType.cast(attributeValue);
        } catch (RuntimeException e) {
            try {
                if (conversionService.canConvert(attributeValue.getClass(), targetType)) {
                    return conversionService.convert(attributeValue, targetType);
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

}
