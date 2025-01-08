/**
 * Copyright 2017-2025 Fred Feng (paganini.fy@gmail.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.github.fastjpa;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * 
 * @Description: FieldList
 * @Author: Fred Feng
 * @Date: 07/10/2024
 * @Version 1.0.0
 */
public class FieldList extends ArrayList<Field<?>> {

    private static final long serialVersionUID = 1L;

    public FieldList() {}

    public FieldList(Field<?>... fields) {
        addAll(Arrays.asList(fields));
    }

    public FieldList addField(String attributeName) {
        add(Property.forName(attributeName));
        return this;
    }

    public FieldList addFields(String[] attributeNames) {
        if (attributeNames != null) {
            for (String attributeName : attributeNames) {
                add(Property.forName(attributeName));
            }
        }
        return this;
    }

    public FieldList addField(String alias, String attributeName) {
        add(Property.forName(alias, attributeName));
        return this;
    }

    public FieldList addFields(String alias, String[] attributeNames) {
        if (attributeNames != null) {
            for (String attributeName : attributeNames) {
                add(Property.forName(alias, attributeName));
            }
        }
        return this;
    }

    public FieldList addField(String attributeName, Class<?> requiredType) {
        add(Property.forName(attributeName, requiredType));
        return this;
    }

    public FieldList addField(String alias, String attributeName, Class<?> requiredType) {
        add(Property.forName(alias, attributeName, requiredType));
        return this;
    }

}
