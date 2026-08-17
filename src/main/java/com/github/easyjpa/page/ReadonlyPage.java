package com.github.easyjpa.page;

import java.util.List;

/**
 * Adapt a PageResponse to the Spring Data Page.
 * 
 * @Description: ReadonlyPage
 * @Author: Fred Feng
 * @Date: 08/03/2023
 * @Version 1.0.0
 */
public class ReadonlyPage<T> implements EachPage<T> {

    private final List<T> content;
    private final PageResponse<T> pageResponse;

    ReadonlyPage(List<T> content, PageResponse<T> pageResponse) {
        this.content = content;
        this.pageResponse = pageResponse;
    }

    @Override
    public boolean isEmpty() {
        return pageResponse.isEmpty();
    }

    @Override
    public boolean isLastPage() {
        return pageResponse.isLastPage();
    }

    @Override
    public boolean isFirstPage() {
        return pageResponse.isFirstPage();
    }

    @Override
    public boolean hasNextPage() {
        return pageResponse.hasNextPage();
    }

    @Override
    public boolean hasPreviousPage() {
        return pageResponse.hasPreviousPage();
    }

    @Override
    public int getTotalPages() {
        return pageResponse.getTotalPages();
    }

    @Override
    public long getTotalRecords() {
        return pageResponse.getTotalRecords();
    }

    @Override
    public long getOffset() {
        return pageResponse.getOffset();
    }

    @Override
    public int getPageSize() {
        return pageResponse.getPageSize();
    }

    @Override
    public int getPageNumber() {
        return pageResponse.getPageNumber();
    }

    @Override
    public List<T> getContent() {
        return content;
    }
}
