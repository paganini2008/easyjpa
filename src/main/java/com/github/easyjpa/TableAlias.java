package com.github.easyjpa;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * Cached Table Alias
 *
 * <p>
 * The library itself no longer keeps the aliases here. A lambda is resolved against the model of
 * the statement being built, so an alias lives as long as that statement rather than as long as the
 * thread. This class is kept for whoever wants to pass an alias around on their own.
 * </p>
 * 
 * @Author: Fred Feng
 * @Date: 07/10/2024
 * @Version 1.0.0
 */
@Deprecated
public final class TableAlias {



    private static final InheritableThreadLocal<Map<String, String>> cache =
            new InheritableThreadLocal<Map<String, String>>() {

                protected Map<String, String> initialValue() {
                    return new HashMap<>();
                }
            };

    public static void put(Type type, String alias) {
        cache.get().put(type.getTypeName(), alias);
    }

    public static String get(String typeName) {
        return cache.get().get(typeName);
    }

    public static String get(Type type) {
        return cache.get().get(type.getTypeName());
    }

    public static void clear() {
        cache.remove();
    }

    public static Map<String, String> copy() {
        return new HashMap<String, String>(cache.get());
    }

}
