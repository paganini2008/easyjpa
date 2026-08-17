package com.github.easyjpa;

import java.util.List;
import jakarta.persistence.EntityNotFoundException;

/**
 * 
 * ListableQuery based on limit and offset
 * 
 * @Author: Fred Feng
 * @Date: 20/10/2024
 * @Version 1.0.0
 */
public interface ListableQuery<T> {

    default List<T> list() {
        return list(-1);
    }

    default List<T> list(int maxResults) {
        return list(maxResults, 0);
    }

    default T first() {
        return first(false);
    }

    default T first(boolean thrown) {
        List<T> list = list(1);
        if (list != null && list.size() > 0) {
            return list.get(0);
        } else if (thrown) {
            throw new EntityNotFoundException();
        }
        return null;
    }

    /** A non positive maxResults means no limit, and a negative firstResult means no offset. */
    List<T> list(int maxResults, long firstResult);
}
