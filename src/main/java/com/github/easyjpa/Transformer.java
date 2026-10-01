
package com.github.easyjpa;

import java.util.List;
import jakarta.persistence.criteria.Selection;

/**
 * 
 * Transformer for Entity Property to other object like map, list, pojo
 * 
 * @Author: Fred Feng
 * @Date: 18/10/2024
 * @Version 1.0.0
 */
public interface Transformer<T, R> {

    R transfer(Model<?> model, T original);

    R transfer(Model<?> model, List<Selection<?>> selections, T original);

}
