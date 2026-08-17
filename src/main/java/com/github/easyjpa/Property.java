package com.github.easyjpa;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;
import com.github.easyjpa.LambdaUtils.LambdaInfo;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;

/**
 * 
 * A named attribute of an entity, which is the most common Field.
 * 
 * @Description: Property
 * @Author: Fred Feng
 * @Date: 07/10/2024
 * @Version 1.0.0
 */
public final class Property<T> implements Field<T> {

    private final String alias;
    private final String declaringClassName;
    private final String attributeName;
    private final Class<T> requiredType;

    Property(String alias, String attributeName, Class<T> requiredType) {
        this(alias, null, attributeName, requiredType);
    }

    Property(String alias, String declaringClassName, String attributeName, Class<T> requiredType) {
        this.alias = StringUtils.isNotBlank(alias) ? alias : null;
        this.declaringClassName = declaringClassName;
        this.attributeName = attributeName;
        this.requiredType = requiredType;
    }

    /**
     * A lambda names no table, it only names the entity the attribute belongs to, so the alias is
     * looked up in the model being built rather than kept anywhere global. That is what makes an
     * outer query and its subquery tell their tables apart even when both query the same entity.
     */
    private String getAlias(Model<?> model) {
        if (alias != null) {
            return alias;
        }
        String resolved = declaringClassName != null ? model.aliasOf(declaringClassName) : null;
        return resolved != null ? resolved : Model.ROOT;
    }

    public Expression<T> toExpression(Model<?> model, CriteriaBuilder builder) {
        String attr = this.attributeName;
        if (model.isAssociatedAttribute(attr, requiredType)) {
            attr = attr + ".id";
        }
        Expression<T> expression = model.getAttribute(getAlias(model), attr);
        if (requiredType != null && supportedJavaType(requiredType)) {
            return expression.as(requiredType);
        }
        return expression;
    }

    protected boolean supportedJavaType(Class<T> requiredType) {
        if (Boolean.class.equals(requiredType) || Character.class.equals(requiredType)
                || CharSequence.class.isAssignableFrom(requiredType)
                || Number.class.isAssignableFrom(requiredType)
                || Date.class.isAssignableFrom(requiredType)
                || Calendar.class.isAssignableFrom(requiredType)
                || LocalDate.class.equals(requiredType) || LocalDateTime.class.equals(requiredType)
                || LocalTime.class.equals(requiredType)) {
            return true;
        }
        return false;
    }

    public String toString() {
        return String.format("%s.%s", alias != null ? alias : Model.ROOT, attributeName);
    }

    public static <T> Property<T> forName(String alias, String attributeName) {
        return forName(alias, attributeName, null);
    }

    public static <T> Property<T> forName(String alias, String attributeName,
            Class<T> requiredType) {
        return new Property<T>(alias, attributeName, requiredType);
    }

    public static <E, T> Property<T> forName(SerializableFunction<E, T> function) {
        return forName(function, null);
    }

    @SuppressWarnings("unchecked")
    public static <E, T> Property<T> forName(SerializableFunction<E, ?> function,
            Class<T> requiredType) {
        LambdaInfo info = LambdaUtils.inspect(function);
        return new Property<T>(null, info.getClassName(), info.getAttributeName(),
                requiredType != null ? requiredType : (Class<T>) info.getAttributeType());
    }

}
