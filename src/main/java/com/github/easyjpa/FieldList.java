package com.github.easyjpa;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * A list of Fields, mostly used by a group by clause.
 * 
 * @Description: FieldList
 * @Author: Fred Feng
 * @Date: 07/10/2024
 * @Version 1.0.0
 */
public class FieldList extends ArrayList<Field<?>> {

    private static final long serialVersionUID = 1992730401503253851L;

    public FieldList() {}

    @SafeVarargs
    public FieldList(Field<?>... fields) {
        super(List.of(fields));
    }

    @SafeVarargs
    public <X> FieldList(SerializableFunction<X, ?>... functions) {
        if (functions != null && functions.length > 0) {
            for (SerializableFunction<X, ?> function : functions) {
                add(Property.forName(function));
            }
        }
    }

    public FieldList(String... attributeNames) {
        if (attributeNames != null && attributeNames.length > 0) {
            for (String attributeName : attributeNames) {
                add(Property.forName(null, attributeName));
            }
        }
    }

    public FieldList(String alias, String[] attributeNames) {
        if (attributeNames != null && attributeNames.length > 0) {
            for (String attributeName : attributeNames) {
                add(Property.forName(alias, attributeName));
            }
        }
    }

    public FieldList addField(String attributeName) {
        add(Property.forName(null, attributeName));
        return this;
    }

    public FieldList addField(String alias, String attributeName) {
        add(Property.forName(alias, attributeName));
        return this;
    }

    public FieldList addField(String attributeName, Class<?> requiredType) {
        add(Property.forName(null, attributeName, requiredType));
        return this;
    }

    public FieldList addField(String alias, String attributeName, Class<?> requiredType) {
        add(Property.forName(alias, attributeName, requiredType));
        return this;
    }

    public FieldList addFields(Field<?>... fields) {
        addAll(List.of(fields));
        return this;
    }

    @SafeVarargs
    public final <X> FieldList addFields(SerializableFunction<X, ?>... functions) {
        addAll(List.of(functions).stream().map(Property::forName).toList());
        return this;
    }

}
