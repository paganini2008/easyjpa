package com.github.easyjpa.page;

import java.util.List;
import java.util.function.Consumer;
import org.springframework.data.domain.Page;

/**
 * 
 * Paginated result, which navigates between the pages.
 * 
 * @Description: PageResponse
 * @Author: Fred Feng
 * @Date: 08/03/2023
 * @Version 1.0.0
 */
public interface PageResponse<T> extends Iterable<EachPage<T>> {

    boolean isEmpty();

    boolean isLastPage();

    boolean isFirstPage();

    boolean hasNextPage();

    boolean hasPreviousPage();

    int getTotalPages();

    long getTotalRecords();

    long getOffset();

    int getPageSize();

    int getPageNumber();

    List<T> getContent();

    PageResponse<T> setPage(int pageNumber);

    PageResponse<T> lastPage();

    PageResponse<T> firstPage();

    PageResponse<T> nextPage();

    PageResponse<T> previousPage();

    Page<T> toPage() throws Exception;

    void forEachPage(Consumer<EachPage<T>> consumer);

}
