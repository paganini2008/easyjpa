
package com.github.easyjpa.hibernate;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;

/**
 * 
 * Extract a native query result with a RowMapper.
 * 
 * @Description: QueryResultSetExtractor
 * @Author: Fred Feng
 * @Date: 18/08/2021
 * @Version 1.0.0
 */
public interface QueryResultSetExtractor<T> {

    List<T> extractData(Session session, NativeQuery<?> query);

}
