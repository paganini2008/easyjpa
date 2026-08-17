package com.github.easyjpa;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.util.ClassUtils;

/**
 * 
 * LambdaUtils will parse lambda expressions like User::getUsername
 * 
 * @Author: Fred Feng
 * @Date: 07/10/2024
 * @Version 1.0.0
 */
public abstract class LambdaUtils {

    private static final Map<String, Class<?>> attributeTypeCache =
            new ConcurrentHashMap<String, Class<?>>();

    public static <E, T> LambdaInfo inspect(SerializableFunction<E, T> function) {
        SerializedLambda object;
        try {
            Method method = function.getClass().getDeclaredMethod("writeReplace");
            method.setAccessible(true);
            object = (SerializedLambda) method.invoke(function);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        String className = object.getImplClass().replace('/', '.');
        String methodName = object.getImplMethodName();
        String attributeName;
        if (methodName.startsWith("is")) {
            attributeName = methodName.substring(2);
        } else if (methodName.startsWith("get") || methodName.startsWith("set")) {
            attributeName = methodName.substring(3);
        } else {
            throw new IllegalArgumentException("Error parsing property name '" + methodName
                    + "'.  Didn't start with 'is', 'get' or 'set'.");
        }
        if (attributeName.length() == 1 || (attributeName.length() > 1
                && !Character.isUpperCase(attributeName.charAt(1)))) {
            attributeName = attributeName.substring(0, 1).toLowerCase(Locale.ENGLISH)
                    + attributeName.substring(1);
        }
        LambdaInfo lambdaInfo = new LambdaInfo();
        lambdaInfo.setClassName(className);
        lambdaInfo.setAttributeName(attributeName);

        String key = className + "." + attributeName;
        Class<?> attributeType = attributeTypeCache.get(key);
        if (attributeType == null) {
            attributeTypeCache.putIfAbsent(key, getAttributeType(className, attributeName));
            attributeType = attributeTypeCache.get(key);
        }
        lambdaInfo.setAttributeType(attributeType);
        return lambdaInfo;
    }

    private static Class<?> getAttributeType(String className, String attributeName) {
        Field field;
        try {
            Class<?> clz = ClassUtils.forName(className, null);
            field = clz.getDeclaredField(attributeName);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericType;
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            if (actualTypeArguments.length > 0) {
                return (Class<?>) actualTypeArguments[0];
            }
        }
        return field.getType();
    }

    public static class LambdaInfo {

        private String className;
        private String attributeName;
        private Class<?> attributeType;

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public String getAttributeName() {
            return attributeName;
        }

        public void setAttributeName(String attributeName) {
            this.attributeName = attributeName;
        }

        public Class<?> getAttributeType() {
            return attributeType;
        }

        public void setAttributeType(Class<?> attributeType) {
            this.attributeType = attributeType;
        }

        @Override
        public String toString() {
            return "className: " + className + ", attributeName: " + attributeName
                    + ", attributeType: " + attributeType;
        }

    }

}
