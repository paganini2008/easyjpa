package com.github.easyjpa.page;

import org.springframework.data.domain.AbstractPageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Page number, which is one based, and page size.
 * 
 * @Description: PageRequest
 * @Author: Fred Feng
 * @Date: 08/03/2023
 * @Version 1.0.0
 */
public class PageRequest extends AbstractPageRequest {

    private static final long serialVersionUID = -7011555825937093676L;

    PageRequest(int pageNumber, int pageSize) {
        super(pageNumber, pageSize);
    }

    public long getOffset() {
        return (getPageNumber() - 1) * getPageSize();
    }

    @Override
    public boolean hasPrevious() {
        return getPageNumber() > 0;
    }

    public Pageable next() {
        return new PageRequest(getPageNumber() + 1, getPageSize());
    }

    public Pageable previous() {
        return getPageNumber() == 1 ? this : new PageRequest(getPageNumber() - 1, getPageSize());
    }

    public Pageable first() {
        return new PageRequest(1, getPageSize());
    }

    public Pageable withPage(int page) {
        return new PageRequest(page, getPageSize());
    }

    @Override
    public Sort getSort() {
        throw new UnsupportedOperationException("getSort");
    }

    public String toString() {
        return "Page: " + getPageNumber() + ", Size: " + getPageSize();
    }

    public static PageRequest of(int size) {
        return of(1, size);
    }

    public static PageRequest of(int page, int size) {
        return new PageRequest(page, size);
    }
}
