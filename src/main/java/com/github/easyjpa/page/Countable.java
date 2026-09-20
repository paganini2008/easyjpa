package com.github.easyjpa.page;

/**
 * Something whose total rows can be counted.
 * 
 * @Description: Countable
 * @Author: Fred Feng
 * @Date: 08/03/2023
 * @Version 1.0.0
 */
public interface Countable {

    /** The total rows of the query, or the total groups if it is a grouping one. */
    default long rowCount() throws Exception {
        return Integer.MAX_VALUE;
    }
}
