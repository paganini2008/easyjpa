package com.github.easyjpa;

import org.springframework.core.convert.support.DefaultConversionService;

/**
 * 
 * Value conversion between the query result and the required java type.
 * 
 * @Description: ConvertUtils
 * @Author: Fred Feng
 * @Date: 23/03/2025
 * @Version 1.0.0
 */
public abstract class ConvertUtils {

    private static final DefaultConversionService conversionService =
            new DefaultConversionService();

    public static <T> T convertValue(Object result, Class<T> requiredType) {
        if (result == null) {
            return null;
        }
        if (conversionService.canConvert(result.getClass(), requiredType)) {
            return conversionService.convert(result, requiredType);
        }
        throw new UnsupportedOperationException("Unsupported target type: " + requiredType
                + ", original type: " + result.getClass());
    }


}
