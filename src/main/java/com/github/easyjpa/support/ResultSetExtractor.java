
package com.github.easyjpa.support;

import jakarta.persistence.Query;

/**
 * 
 * Extract the data from a native query.
 * 
 * @Description: ResultSetExtractor
 * @Author: Fred Feng
 * @Date: 18/08/2021
 * @Version 1.0.0
 */
public interface ResultSetExtractor<T> {

    T extractData(Query query);

}
