package com.github.easyjpa;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;

/**
 * 
 * Fields utilities
 * 
 * @Author: Fred Feng
 * @Date: 07/10/2024
 * @Version 1.0.0
 */
public abstract class Fields {

    public static <X> Field<X> root() {
        return new Field<X>() {

            @SuppressWarnings("unchecked")
            public Expression<X> toExpression(Model<?> model, CriteriaBuilder builder) {
                return (Expression<X>) model.getRoot();
            }

            public String toString() {
                return Model.ROOT;
            }
        };
    }

    public static Field<BigDecimal> toBigDecimal(BigDecimal value) {
        return new Field<BigDecimal>() {

            public Expression<BigDecimal> toExpression(Model<?> model, CriteriaBuilder builder) {
                return builder.toBigDecimal(builder.literal(value));
            }

            public String toString() {
                return value.toPlainString();
            }
        };
    }

    public static Field<BigInteger> toBigInteger(BigInteger value) {
        return new Field<BigInteger>() {

            public Expression<BigInteger> toExpression(Model<?> model, CriteriaBuilder builder) {
                return builder.toBigInteger(builder.literal(value));
            }

            public String toString() {
                return value.toString();
            }
        };
    }

    public static Field<Integer> toInteger(Integer value) {
        return new Field<Integer>() {

            public Expression<Integer> toExpression(Model<?> model, CriteriaBuilder builder) {
                return builder.toInteger(builder.literal(value));
            }

            public String toString() {
                return value.toString();
            }
        };
    }

    public static Field<Long> toLong(Long value) {
        return new Field<Long>() {

            public Expression<Long> toExpression(Model<?> model, CriteriaBuilder builder) {
                return builder.toLong(builder.literal(value));
            }

            public String toString() {
                return value.toString();
            }
        };
    }

    public static Field<Float> toFloat(Float value) {

        return new Field<Float>() {

            public Expression<Float> toExpression(Model<?> model, CriteriaBuilder builder) {
                return builder.toFloat(builder.literal(value));
            }

            public String toString() {
                return value.toString();
            }
        };
    }

    public static Field<Double> toDouble(Double value) {
        return new Field<Double>() {

            public Expression<Double> toExpression(Model<?> model, CriteriaBuilder builder) {
                return builder.toDouble(builder.literal(value));
            }

            public String toString() {
                return value.toString();
            }
        };
    }

    public static Field<String> toString(Character value) {
        return new Field<String>() {

            public Expression<String> toExpression(Model<?> model, CriteriaBuilder builder) {
                return builder.toString(builder.literal(value));
            }

            public String toString() {
                return value.toString();
            }
        };
    }

    public static <T> Field<T> toLiteral(T value) {
        return new Field<T>() {

            public Expression<T> toExpression(Model<?> model, CriteriaBuilder builder) {
                return builder.literal(value);
            }

            public String toString() {
                return value.toString();
            }
        };
    }

    public static <E, T extends Number> Field<T> plus(SerializableFunction<E, T> leftFunction,
                                                      SerializableFunction<E, T> rightFunction) {
        return plus(Property.forName(leftFunction), Property.forName(rightFunction));
    }

    public static <E, T extends Number> Field<T> plus(SerializableFunction<E, T> leftFunction,
                                                      Field<T> rightField) {
        return plus(Property.forName(leftFunction), rightField);
    }

    public static <T extends Number> Field<T> plus(Field<T> leftField, Field<T> rightField) {
        return new Field<T>() {

            public Expression<T> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<T> leftExpression = leftField.toExpression(model, builder);
                Expression<T> rightExpression = rightField.toExpression(model, builder);
                return builder.sum(leftExpression, rightExpression);
            }

            public String toString() {
                return leftField.toString() + "+" + rightField.toString();
            }
        };
    }

    public static <E, T extends Number> Field<T> plusValue(SerializableFunction<E, T> leftFunction,
                                                           T value) {
        return plusValue(Property.forName(leftFunction), value);
    }

    public static <T extends Number> Field<T> plusValue(Field<T> field, T value) {
        return new Field<T>() {

            public Expression<T> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<T> expression = field.toExpression(model, builder);
                return builder.sum(expression, value);
            }

            public String toString() {
                return field.toString() + "+" + value;
            }
        };
    }

    public static <E, T extends Number> Field<T> minus(SerializableFunction<E, T> leftfFunction,
                                                       SerializableFunction<E, T> rightFunction) {
        return minus(Property.forName(leftfFunction), Property.forName(rightFunction));
    }

    public static <E, T extends Number> Field<T> minus(SerializableFunction<E, T> leftFunction,
                                                       Field<T> rightField) {
        return minus(Property.forName(leftFunction), rightField);
    }

    public static <T extends Number> Field<T> minus(Field<T> leftField, Field<T> rightField) {
        return new Field<T>() {

            public Expression<T> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<T> leftExpression = leftField.toExpression(model, builder);
                Expression<T> rightExpression = rightField.toExpression(model, builder);
                return builder.diff(leftExpression, rightExpression);
            }

            public String toString() {
                return leftField.toString() + "-" + rightField.toString();
            }
        };
    }

    public static <E, T extends Number> Field<T> minusValue(SerializableFunction<E, T> field,
                                                            T value) {
        return minusValue(Property.forName(field), value);
    }

    public static <T extends Number> Field<T> minusValue(Field<T> field, T value) {
        return new Field<T>() {

            public Expression<T> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<T> expression = field.toExpression(model, builder);
                return builder.diff(expression, value);
            }

            public String toString() {
                return field.toString() + "-" + value;
            }
        };
    }

    public static <E, T extends Number> Field<T> multiply(SerializableFunction<E, T> leftFunction,
                                                          SerializableFunction<E, T> rightfFunction) {
        return multiply(Property.forName(leftFunction), Property.forName(rightfFunction));
    }

    public static <E, T extends Number> Field<T> multiply(SerializableFunction<E, T> leftFunction,
                                                          Field<T> rightField) {
        return multiply(Property.forName(leftFunction), rightField);
    }

    public static <T extends Number> Field<T> multiply(Field<T> leftField, Field<T> rightField) {
        return new Field<T>() {

            public Expression<T> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<T> leftExpression = leftField.toExpression(model, builder);
                Expression<T> rightExpression = rightField.toExpression(model, builder);
                return builder.prod(leftExpression, rightExpression);
            }

            public String toString() {
                return leftField.toString() + "*" + rightField.toString();
            }
        };
    }

    public static <E, T extends Number> Field<T> multiplyValue(SerializableFunction<E, T> field,
                                                               T value) {
        return multiplyValue(Property.forName(field), value);
    }

    public static <T extends Number> Field<T> multiplyValue(Field<T> field, T value) {
        return new Field<T>() {

            public Expression<T> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<T> expression = field.toExpression(model, builder);
                return builder.prod(expression, value);
            }

            public String toString() {
                return field.toString() + "*" + value;
            }
        };
    }

    public static <E, T extends Number> Field<Number> divide(
            SerializableFunction<E, T> leftExpression, SerializableFunction<E, T> rightField) {
        return divide(Property.forName(leftExpression), Property.forName(rightField));
    }


    public static <E, T extends Number> Field<Number> divide(SerializableFunction<E, T> leftFunction,
                                                             Field<T> rightField) {
        return divide(Property.forName(leftFunction), rightField);
    }

    public static <T extends Number> Field<Number> divide(Field<T> leftField, Field<T> rightField) {
        return new Field<Number>() {

            public Expression<Number> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<T> leftExpression = leftField.toExpression(model, builder);
                Expression<T> rightExpression = rightField.toExpression(model, builder);
                return builder.quot(leftExpression, rightExpression);
            }

            public String toString() {
                return leftField.toString() + "/" + rightField.toString();
            }
        };
    }

    public static <E, T extends Number> Field<Number> divideValue(SerializableFunction<E, T> field,
                                                                  T value) {
        return divideValue(Property.forName(field), value);
    }

    public static <T extends Number> Field<Number> divideValue(Field<T> field, T value) {
        return new Field<Number>() {

            public Expression<Number> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<T> expression = field.toExpression(model, builder);
                return builder.quot(expression, value);
            }

            public String toString() {
                return field.toString() + "/" + value;
            }
        };
    }

    public static <E, T extends Number> Field<Number> divide(T value,
            SerializableFunction<E, T> field) {
        return divide(value, Property.forName(field));
    }

    public static <T extends Number> Field<Number> divide(T value, Field<T> field) {
        return new Field<Number>() {

            public Expression<Number> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<T> expression = field.toExpression(model, builder);
                return builder.quot(value, expression);
            }

            public String toString() {
                return value + "/" + field.toString();
            }
        };
    }

    public static Field<Integer> mod(Field<Integer> field, Integer value) {
        return new Field<Integer>() {

            public Expression<Integer> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<Integer> expression = field.toExpression(model, builder);
                return builder.mod(expression, value);
            }

            public String toString() {
                return "mod(" + field.toString() + "," + value + ")";
            }
        };
    }

    public static Field<Integer> mod(Integer value, Field<Integer> field) {
        return new Field<Integer>() {

            public Expression<Integer> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<Integer> expression = field.toExpression(model, builder);
                return builder.mod(value, expression);
            }

            public String toString() {
                return "mod(" + value + "," + field.toString() + ")";
            }
        };
    }

    public static Field<Integer> mod(Field<Integer> field, Field<Integer> otherField) {
        return new Field<Integer>() {

            public Expression<Integer> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<Integer> expression = field.toExpression(model, builder);
                return builder.mod(expression, otherField.toExpression(model, builder));
            }

            public String toString() {
                return "mod(" + field.toString() + "," + otherField.toString() + ")";
            }
        };
    }

    public static <X, T extends Number> Field<T> neg(SerializableFunction<X, T> function) {
        return neg(Property.forName(function));
    }

    public static <T extends Number> Field<T> neg(Field<T> field) {
        return new Field<T>() {

            public Expression<T> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<T> expression = field.toExpression(model, builder);
                return builder.neg(expression);
            }

            public String toString() {
                return "-1 * " + field.toString();
            }
        };
    }

    public static <X, T extends Number> Field<Double> sqrt(SerializableFunction<X, T> function) {
        return sqrt(Property.forName(function));
    }

    public static <T extends Number> Field<Double> sqrt(Field<T> field) {
        return new Field<Double>() {

            public Expression<Double> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<T> expression = field.toExpression(model, builder);
                return builder.sqrt(expression);
            }

            public String toString() {
                return "sqrt(" + field.toString() + ")";
            }
        };
    }

    public static <X, T extends Number> Field<T> abs(SerializableFunction<X, T> function) {
        return abs(Property.forName(function));
    }

    public static <T extends Number> Field<T> abs(Field<T> field) {
        return new Field<T>() {

            public Expression<T> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<T> expression = field.toExpression(model, builder);
                return builder.abs(expression);
            }

            public String toString() {
                return "abs(" + field.toString() + ")";
            }
        };
    }

    public static <X> Field<Integer> year(SerializableFunction<X, ?> function) {
        return year(Property.forName(function));
    }

    public static Field<Integer> year(String attributeName) {
        return year(Property.forName(null, attributeName));
    }

    public static Field<Integer> year(String alias, String attributeName) {
        return year(Property.forName(alias, attributeName));
    }

    /**
     * The year of a date or a timestamp, which every database spells its own way and Hibernate
     * renders accordingly.
     */
    public static Field<Integer> year(Field<?> field) {
        return new Field<Integer>() {

            public Expression<Integer> toExpression(Model<?> model, CriteriaBuilder builder) {
                return JpaProviders.getProvider().extract(builder, field.toExpression(model, builder),
                        JpaProvider.DatePart.YEAR);
            }

            public String toString() {
                return String.format("year(%s)", field.toString());
            }
        };
    }

    public static <X> Field<Integer> month(SerializableFunction<X, ?> function) {
        return month(Property.forName(function));
    }

    public static Field<Integer> month(String attributeName) {
        return month(Property.forName(null, attributeName));
    }

    public static Field<Integer> month(String alias, String attributeName) {
        return month(Property.forName(alias, attributeName));
    }

    /**
     * The month of a date or a timestamp, which every database spells its own way and Hibernate
     * renders accordingly.
     */
    public static Field<Integer> month(Field<?> field) {
        return new Field<Integer>() {

            public Expression<Integer> toExpression(Model<?> model, CriteriaBuilder builder) {
                return JpaProviders.getProvider().extract(builder, field.toExpression(model, builder),
                        JpaProvider.DatePart.MONTH);
            }

            public String toString() {
                return String.format("month(%s)", field.toString());
            }
        };
    }

    public static <X> Field<Integer> day(SerializableFunction<X, ?> function) {
        return day(Property.forName(function));
    }

    public static Field<Integer> day(String attributeName) {
        return day(Property.forName(null, attributeName));
    }

    public static Field<Integer> day(String alias, String attributeName) {
        return day(Property.forName(alias, attributeName));
    }

    /**
     * The day of a date or a timestamp, which every database spells its own way and Hibernate
     * renders accordingly.
     */
    public static Field<Integer> day(Field<?> field) {
        return new Field<Integer>() {

            public Expression<Integer> toExpression(Model<?> model, CriteriaBuilder builder) {
                return JpaProviders.getProvider().extract(builder, field.toExpression(model, builder),
                        JpaProvider.DatePart.DAY);
            }

            public String toString() {
                return String.format("day(%s)", field.toString());
            }
        };
    }

    public static <X, T extends Comparable<T>> Field<T> max(SerializableFunction<X, T> function) {
        return max(Property.forName(function));
    }

    public static <T extends Comparable<T>> Field<T> max(String alias, String attributeName,
            Class<T> requiredType) {
        return max(Property.forName(alias, attributeName, requiredType));
    }

    public static <T extends Comparable<T>> Field<T> max(String attributeName,
            Class<T> requiredType) {
        return max(Property.forName(null, attributeName, requiredType));
    }

    public static <T extends Comparable<T>> Field<T> max(Field<T> field) {
        return new Field<T>() {

            public Expression<T> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<T> expression = field.toExpression(model, builder);
                return builder.greatest(expression);
            }

            public String toString() {
                return "max(" + field.toString() + ")";
            }
        };
    }

    public static <X, T extends Comparable<T>> Field<T> min(SerializableFunction<X, T> function) {
        return min(Property.forName(function));
    }

    public static <T extends Comparable<T>> Field<T> min(String alias, String attributeName,
            Class<T> requiredType) {
        return min(Property.forName(alias, attributeName, requiredType));
    }

    public static <T extends Comparable<T>> Field<T> min(String attributeName,
            Class<T> requiredType) {
        return min(Property.forName(null, attributeName, requiredType));
    }

    public static <T extends Comparable<T>> Field<T> min(Field<T> field) {
        return new Field<T>() {

            public Expression<T> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<T> expression = field.toExpression(model, builder);
                return builder.least(expression);
            }

            public String toString() {
                return "min(" + field.toString() + ")";
            }
        };
    }

    public static <X, T extends Number> Field<T> sum(SerializableFunction<X, T> function) {
        return sum(Property.forName(function));
    }

    public static <T extends Number> Field<T> sum(String alias, String attributeName,
            Class<T> requiredType) {
        return sum(Property.forName(alias, attributeName, requiredType));
    }

    public static <T extends Number> Field<T> sum(String attributeName, Class<T> requiredType) {
        return sum(Property.forName(null, attributeName, requiredType));
    }

    public static <T extends Number> Field<T> sum(Field<T> field) {
        return new Field<T>() {

            public Expression<T> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<T> expression = field.toExpression(model, builder);
                return builder.sum(expression);
            }

            public String toString() {
                return "sum(" + field.toString() + ")";
            }
        };
    }

    public static <X, T extends Number> Field<Double> avg(SerializableFunction<X, T> function) {
        return avg(Property.forName(function));
    }

    public static <T extends Number> Field<Double> avg(String alias, String attributeName,
            Class<T> requiredType) {
        return avg(Property.forName(alias, attributeName, requiredType));
    }

    public static <T extends Number> Field<Double> avg(String attributeName,
            Class<T> requiredType) {
        return avg(Property.forName(null, attributeName, requiredType));
    }

    public static <T extends Number> Field<Double> avg(Field<T> field) {
        return new Field<Double>() {

            public Expression<Double> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<T> expression = field.toExpression(model, builder);
                return builder.avg(expression);
            }

            public String toString() {
                return "avg(" + field.toString() + ")";
            }
        };
    }

    public static Field<Long> count(long l) {
        return count(toLong(l));
    }

    public static Field<Long> count(String attributeName) {
        return count(Property.forName(null, attributeName));
    }

    public static Field<Long> count(String alias, String attributeName) {
        return count(Property.forName(alias, attributeName));
    }

    public static <X> Field<Long> count(SerializableFunction<X, ?> function) {
        return count(Property.forName(function));
    }

    public static Field<Long> count(Field<?> field) {
        return new Field<Long>() {

            public Expression<Long> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<?> expression = field.toExpression(model, builder);
                return builder.count(expression);
            }

            public String toString() {
                return "count(" + field.toString() + ")";
            }
        };
    }

    public static Field<Long> count(SubQueryBuilder<?> subQueryBuilder) {
        return new Field<Long>() {

            public Expression<Long> toExpression(Model<?> model, CriteriaBuilder builder) {
                return builder.count(subQueryBuilder.toSubquery(builder));
            }

            public String toString() {
                return "count(subquery)";
            }
        };
    }

    public static Field<Long> countDistinct(String attributeName) {
        return countDistinct(Property.forName(null, attributeName));
    }

    public static Field<Long> countDistinct(String alias, String attributeName) {
        return countDistinct(Property.forName(alias, attributeName));
    }

    public static <X> Field<Long> countDistinct(SerializableFunction<X, ?> function) {
        return countDistinct(Property.forName(function));
    }

    public static Field<Long> countDistinct(Field<?> field) {
        return new Field<Long>() {

            public Expression<Long> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<?> expression = field.toExpression(model, builder);
                return builder.countDistinct(expression);
            }

            public String toString() {
                return "count(distinct " + field.toString() + ")";
            }
        };
    }

    public static Field<Long> countDistinct(SubQueryBuilder<?> subQueryBuilder) {
        return new Field<Long>() {

            public Expression<Long> toExpression(Model<?> model, CriteriaBuilder builder) {
                return builder.countDistinct(subQueryBuilder.toSubquery(builder));
            }

            public String toString() {
                return "count(distinct subquery)";
            }
        };
    }

    public static <T> Field<T> some(SubQueryBuilder<T> subquery) {
        return new Field<T>() {

            public Expression<T> toExpression(Model<?> model, CriteriaBuilder builder) {
                return builder.some(subquery.toSubquery(builder));
            }

            public String toString() {
                return "some (" + subquery + ")";
            }
        };
    }

    public static <T> Field<T> any(SubQueryBuilder<T> subquery) {
        return new Field<T>() {

            public Expression<T> toExpression(Model<?> model, CriteriaBuilder builder) {
                return builder.any(subquery.toSubquery(builder));
            }

            public String toString() {
                return "any (" + subquery + ")";
            }
        };
    }

    public static <T> Field<T> all(SubQueryBuilder<T> subquery) {
        return new Field<T>() {

            public Expression<T> toExpression(Model<?> model, CriteriaBuilder builder) {
                return builder.all(subquery.toSubquery(builder));
            }

            public String toString() {
                return "all (" + subquery + ")";
            }
        };
    }

    public static <T> Field<T> coalesce(Field<T> field, Field<T> anotherField) {
        return new Field<T>() {

            public Expression<T> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<T> expression = field.toExpression(model, builder);
                Expression<T> anotherExpression = field.toExpression(model, builder);
                return builder.coalesce(expression, anotherExpression);
            }

            public String toString() {
                return "coalesce(" + field.toString() + "," + anotherField + ")";
            }
        };
    }

    public static <X, T> Field<T> coalesce(SerializableFunction<X, T> function, T value) {
        return coalesce(Property.forName(function), value);
    }

    public static <T> Field<T> coalesce(Field<T> field, T value) {
        return new Field<T>() {

            public Expression<T> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<T> expression = field.toExpression(model, builder);
                return builder.coalesce(expression, value);
            }

            public String toString() {
                return "coalesce(" + field.toString() + "," + value + ")";
            }
        };
    }

    public static <X, T> Field<T> nullif(SerializableFunction<X, T> function, T value) {
        return nullif(Property.forName(function), value);
    }

    public static <T> Field<T> nullif(Field<T> field, T value) {
        return new Field<T>() {

            public Expression<T> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<T> expression = field.toExpression(model, builder);
                return builder.nullif(expression, value);
            }

            public String toString() {
                return "nullif(" + field.toString() + "," + value + ")";
            }
        };
    }

    public static Field<java.sql.Date> currentDate() {
        return new Field<java.sql.Date>() {

            public Expression<java.sql.Date> toExpression(Model<?> model, CriteriaBuilder builder) {
                return builder.currentDate();
            }

            public String toString() {
                return "current_date()";
            }
        };
    }

    public static Field<java.sql.Time> currentTime() {
        return new Field<java.sql.Time>() {

            public Expression<java.sql.Time> toExpression(Model<?> model, CriteriaBuilder builder) {
                return builder.currentTime();
            }

            public String toString() {
                return "current_time()";
            }
        };
    }

    public static Field<Timestamp> currentTimestamp() {
        return new Field<Timestamp>() {

            public Expression<Timestamp> toExpression(Model<?> model, CriteriaBuilder builder) {
                return builder.currentTimestamp();
            }

            public String toString() {
                return "current_timestamp()";
            }
        };

    }

    public static Field<LocalDate> localDate() {

        return new Field<LocalDate>() {

            public Expression<LocalDate> toExpression(Model<?> model, CriteriaBuilder builder) {
                return builder.localDate();
            }

            public String toString() {
                return "current_date()";
            }
        };

    }

    public static Field<LocalTime> localTime() {

        return new Field<LocalTime>() {

            public Expression<LocalTime> toExpression(Model<?> model, CriteriaBuilder builder) {
                return builder.localTime();
            }

            public String toString() {
                return "current_time()";
            }
        };

    }

    public static Field<LocalDateTime> localDateTime() {

        return new Field<LocalDateTime>() {

            public Expression<LocalDateTime> toExpression(Model<?> model, CriteriaBuilder builder) {
                return builder.localDateTime();
            }

            public String toString() {
                return "current_timestamp()";
            }
        };

    }

    public static Field<String> concat(Field<String> left, Field<String> right) {
        return new Field<String>() {

            public Expression<String> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<String> leftExpression = left.toExpression(model, builder);
                Expression<String> rightExpression = right.toExpression(model, builder);
                return builder.concat(leftExpression, rightExpression);
            }

            public String toString() {
                return "concat(" + left.toString() + "," + right.toString() + ")";
            }
        };

    }

    public static <X> Field<String> concat(String value, SerializableFunction<X, String> function) {
        return concat(value, Property.forName(function));
    }

    public static Field<String> concat(String value, Field<String> field) {
        return new Field<String>() {

            public Expression<String> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<String> expression = field.toExpression(model, builder);
                return builder.concat(value, expression);
            }

            public String toString() {
                return "concat(" + value + "," + field.toString() + ")";
            }
        };

    }

    public static <X> Field<String> concat(SerializableFunction<X, String> function, String value) {
        return concat(Property.forName(function), value);
    }

    public static Field<String> concat(Field<String> field, String value) {
        return new Field<String>() {

            public Expression<String> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<String> expression = field.toExpression(model, builder);
                return builder.concat(expression, value);
            }

            public String toString() {
                return "concat(" + field.toString() + "," + value + ")";
            }
        };

    }

    public static <X> Field<Integer> length(SerializableFunction<X, String> function) {
        return length(Property.forName(function));
    }

    public static Field<Integer> length(Field<String> field) {
        return new Field<Integer>() {

            public Expression<Integer> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<String> expression = field.toExpression(model, builder);
                return builder.length(expression);
            }

            public String toString() {
                return "length(" + field.toString() + ")";
            }
        };

    }

    public static <X, T extends Number> Field<T> ceil(SerializableFunction<X, T> function) {
        return ceil(Property.forName(function));
    }

    public static <T extends Number> Field<T> ceil(Field<T> field) {
        return new Field<T>() {

            public Expression<T> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<T> expression = field.toExpression(model, builder);
                return builder.ceiling(expression);
            }

            public String toString() {
                return "ceil(" + field.toString() + ")";
            }
        };

    }

    public static <X, T extends Number> Field<Integer> sign(SerializableFunction<X, T> function) {
        return sign(Property.forName(function));
    }

    public static <T extends Number> Field<Integer> sign(Field<T> field) {
        return new Field<Integer>() {

            public Expression<Integer> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<T> expression = field.toExpression(model, builder);
                return builder.sign(expression);
            }

            public String toString() {
                return "sign(" + field.toString() + ")";
            }
        };

    }

    public static <X, T extends Number> Field<T> floor(SerializableFunction<X, T> function) {
        return floor(Property.forName(function));
    }

    public static <T extends Number> Field<T> floor(Field<T> field) {
        return new Field<T>() {

            public Expression<T> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<T> expression = field.toExpression(model, builder);
                return builder.floor(expression);
            }

            public String toString() {
                return "floor(" + field.toString() + ")";
            }
        };

    }

    public static <X, T extends Number> Field<T> round(SerializableFunction<X, T> function,
                                                       Integer scale) {
        return round(Property.forName(function), scale);
    }

    public static <T extends Number> Field<T> round(Field<T> field, Integer scale) {
        return new Field<T>() {

            public Expression<T> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<T> expression = field.toExpression(model, builder);
                return builder.round(expression, scale);
            }

            public String toString() {
                return "round(" + field.toString() + "," + scale + ")";
            }
        };

    }

    public static Field<String> substring(Field<String> field, Field<Integer> anotherField) {
        return new Field<String>() {

            public Expression<String> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<String> expression = field.toExpression(model, builder);
                Expression<Integer> anotherExpression = anotherField.toExpression(model, builder);
                return builder.substring(expression, anotherExpression);
            }

            public String toString() {
                return "substring(" + field.toString() + "," + anotherField.toString() + ")";
            }
        };

    }

    public static <X> Field<String> substring(SerializableFunction<X, String> function, int from,
                                              int to) {
        return substring(Property.forName(function), from, to);
    }

    public static Field<String> substring(Field<String> field, int from, int to) {
        return new Field<String>() {

            public Expression<String> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<String> expression = field.toExpression(model, builder);
                return builder.substring(expression, from, to);
            }

            public String toString() {
                return "substring(" + field.toString() + "," + from + "," + to + ")";
            }
        };

    }

    public static <X> Field<String> substring(SerializableFunction<X, String> function, int from) {
        return substring(Property.forName(function), from);
    }

    public static Field<String> substring(Field<String> field, int from) {
        return new Field<String>() {

            public Expression<String> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<String> expression = field.toExpression(model, builder);
                return builder.substring(expression, from);
            }

            public String toString() {
                return "substring(" + field.toString() + "," + from + ")";
            }
        };

    }

    public static <X> Field<String> lower(SerializableFunction<X, String> function) {
        return lower(Property.forName(function));
    }

    public static Field<String> lower(Field<String> field) {
        return new Field<String>() {

            public Expression<String> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<String> expression = field.toExpression(model, builder);
                return builder.lower(expression);
            }

            public String toString() {
                return "lower(" + field.toString() + ")";
            }
        };

    }

    public static <X> Field<String> upper(SerializableFunction<X, String> function) {
        return upper(Property.forName(function));
    }

    public static Field<String> upper(Field<String> field) {
        return new Field<String>() {

            public Expression<String> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<String> expression = field.toExpression(model, builder);
                return builder.upper(expression);
            }

            public String toString() {
                return "upper(" + field.toString() + ")";
            }
        };
    }

    public static <X, T> Field<Boolean> eq(SerializableFunction<X, T> function, T value) {
        return eq(Property.forName(function), value);
    }

    public static <T> Field<Boolean> eq(Field<T> field, T value) {

        return new Field<Boolean>() {

            public Expression<Boolean> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<T> expression = field.toExpression(model, builder);
                return builder.equal(expression, value);
            }

            public String toString() {
                return field.toString();
            }

        };
    }

    public static <X, T> Field<Boolean> ne(SerializableFunction<X, T> function, T value) {
        return ne(Property.forName(function), value);
    }

    public static <T> Field<Boolean> ne(Field<T> field, T value) {

        return new Field<Boolean>() {

            public Expression<Boolean> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<T> expression = field.toExpression(model, builder);
                return builder.notEqual(expression, value);
            }

            public String toString() {
                return field.toString();
            }

        };
    }

    public static <X, T extends Comparable<T>> Field<Boolean> lt(SerializableFunction<X, T> function,
                                                                 T value) {
        return lt(Property.forName(function), value);
    }

    public static <T extends Comparable<T>> Field<Boolean> lt(Field<T> field, T value) {

        return new Field<Boolean>() {

            public Expression<Boolean> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<T> expression = field.toExpression(model, builder);
                return builder.lessThan(expression, value);
            }

            public String toString() {
                return field.toString();
            }

        };
    }

    public static <X, T extends Comparable<T>> Field<Boolean> lte(SerializableFunction<X, T> function,
                                                                  T value) {
        return lte(Property.forName(function), value);
    }

    public static <T extends Comparable<T>> Field<Boolean> lte(Field<T> field, T value) {

        return new Field<Boolean>() {

            public Expression<Boolean> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<T> expression = field.toExpression(model, builder);
                return builder.lessThanOrEqualTo(expression, value);
            }

            public String toString() {
                return field.toString();
            }

        };
    }

    public static <X, T extends Comparable<T>> Field<Boolean> gt(SerializableFunction<X, T> function,
                                                                 T value) {
        return gt(Property.forName(function), value);
    }

    public static <T extends Comparable<T>> Field<Boolean> gt(Field<T> field, T value) {

        return new Field<Boolean>() {

            public Expression<Boolean> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<T> expression = field.toExpression(model, builder);
                return builder.greaterThan(expression, value);
            }

            public String toString() {
                return field.toString();
            }

        };
    }

    public static <X, T extends Comparable<T>> Field<Boolean> gte(SerializableFunction<X, T> function,
                                                                  T value) {
        return gte(Property.forName(function), value);
    }

    public static <T extends Comparable<T>> Field<Boolean> gte(Field<T> field, T value) {

        return new Field<Boolean>() {

            public Expression<Boolean> toExpression(Model<?> model, CriteriaBuilder builder) {
                Expression<T> expression = field.toExpression(model, builder);
                return builder.greaterThanOrEqualTo(expression, value);
            }

            public String toString() {
                return field.toString();
            }

        };
    }

}
