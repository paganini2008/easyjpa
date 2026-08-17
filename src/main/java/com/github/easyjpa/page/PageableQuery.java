package com.github.easyjpa.page;

import org.springframework.data.domain.Pageable;
import com.github.easyjpa.ListableQuery;

/**
 * 
 * A query which can be paginated.
 * 
 * @Description: PageableQuery
 * @Author: Fred Feng
 * @Date: 08/10/2024
 * @Version 1.0.0
 */
public interface PageableQuery<T> extends ListableQuery<T>, Countable {

    default PageResponse<T> paginate(Pageable pageable) {
        return new PageResponseImpl<T>(pageable, this);
    }
}
