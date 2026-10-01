package com.github.easyjpa;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Selection;

/**
 * 
 * Represent column list
 * 
 * @Author: Fred Feng
 * @Date: 07/10/2024
 * @Version 1.0.0
 */
public class ColumnList extends ArrayList<Column> {

    private static final long serialVersionUID = -1066600234659676707L;

    public ColumnList() {}

    public ColumnList(Column... columns) {
        super(List.of(columns));
    }

    public ColumnList(String[] attributeNames) {
        if (attributeNames != null && attributeNames.length > 0) {
            for (String attributeName : attributeNames) {
                add(Column.forName(attributeName));
            }
        }
    }

    @SafeVarargs
    public <X> ColumnList(SerializableFunction<X, ?>... functions) {
        if (functions != null && functions.length > 0) {
            addColumns(functions);
        }
    }

    @SafeVarargs
    public ColumnList(Field<?>... fields) {
        if (fields != null && fields.length > 0) {
            addFields(fields);
        }
    }

    public ColumnList(String alias, String[] attributeNames) {
        if (attributeNames != null && attributeNames.length > 0) {
            for (String attributeName : attributeNames) {
                add(Column.forName(alias, attributeName));
            }
        }
    }

    public ColumnList addColumn(String attributeName) {
        add(Column.forName(attributeName));
        return this;
    }

    public ColumnList addColumn(String attributeName, Class<?> requiredType) {
        add(Column.forName(attributeName, requiredType));
        return this;
    }

    public ColumnList addColumn(String alias, String attributeName, Class<?> requiredType) {
        add(Column.forName(alias, attributeName, requiredType));
        return this;
    }

    public ColumnList addColumn(String alias, String attributeName) {
        add(Column.forName(alias, attributeName));
        return this;
    }

    public ColumnList addFields(Field<?>... fields) {
        addAll(List.of(fields).stream().map(f -> f.as(f.toString())).toList());
        return this;
    }

    public ColumnList addColumns(Column... columns) {
        addAll(List.of(columns));
        return this;
    }

    @SafeVarargs
    public final <X> ColumnList addColumns(SerializableFunction<X, ?>... functions) {
        addAll(List.of(functions).stream().map(f -> Column.forName(f, null)).toList());
        return this;
    }

    public ColumnList addTableAlias(String... tableAliases) {
        addAll(List.of(tableAliases).stream().map(ta -> {
            return new Column() {
                @Override
                public Selection<?> toSelection(Model<?> model, CriteriaBuilder builder) {
                    return model.getSelection(ta);
                }
            };
        }).toList());
        return this;
    }

}
