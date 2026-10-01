package com.github.easyjpa;

import jakarta.persistence.criteria.Path;

/**
 * 
 * Create a Path from a nested attribute name such as "user.id".
 * 
 * @Description: PathUtils
 * @Author: Fred Feng
 * @Date: 07/10/2024
 * @Version 1.0.0
 */
public abstract class PathUtils {

    public static <X, T> Path<T> createPath(Path<X> root, String property) {
        Path<T> path = null;
        int index = property.indexOf(".");
        if (index > 0) {
            path = root.get(property.substring(0, index));
            String[] names = property.substring(index + 1).split("\\.");
            for (int i = 0; i < names.length; i++) {
                path = path.get(names[i]);
            }
        } else {
            path = root.get(property);
        }
        return path;
    }

}
