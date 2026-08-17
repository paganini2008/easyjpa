
package com.github.easyjpa.support;

import java.util.TreeMap;
import java.util.Map;
import com.github.easyjpa.page.PageableQuery;

/**
 * 
 * Native sql operations for the cases the Criteria API does not cover.
 * 
 * @Description: NativeSqlOperations
 * @Author: Fred Feng
 * @Date: 23/08/2021
 * @Version 1.0.0
 */
public interface NativeSqlOperations<E> {

    PageableQuery<E> query(String sql, Object[] arguments);

    <T> T getSingleResult(String sql, Object[] arguments, Class<T> requiredType);

    <T> PageableQuery<T> query(String sql, Object[] arguments, Class<T> resultClass);

    <T> PageableQuery<T> query(String sql, Object[] arguments, RowMapper<T> rowMapper);

    /** The keys of the returned maps are the column labels, which are matched case insensitively. */
    default PageableQuery<Map<String, Object>> queryForMap(String sql, Object[] arguments) {
        return query(sql, arguments, (index, data) -> {
            Map<String, Object> copy = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
            copy.putAll(data);
            return copy;
        });
    }

    <T> T execute(String sql, Object[] arguments, ResultSetExtractor<T> extractor);

    int executeUpdate(String sql, Object[] arguments);

}
