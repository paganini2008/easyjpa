package com.github.easyjpa.page;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * Default PageResponse implementation.
 * 
 * @Description: PageResponseImpl in Generic paging tools
 * @Author: Fred Feng
 * @Date: 08/03/2023
 * @Version 1.0.0
 */
public class PageResponseImpl<T> implements PageResponse<T>, Serializable {

    private static final long serialVersionUID = 3421565187066969539L;

    private final int pageNumber;
    private final int totalPages;
    private final long totalRecords;
    private final Pageable pageable;
    private final PageableQuery<T> paginator;

    PageResponseImpl(Pageable pageable, PageableQuery<T> paginator) {
        this.pageNumber = pageable.getPageNumber();
        try {
            this.totalRecords = paginator.rowCount();
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        this.totalPages =
                (int) ((totalRecords + pageable.getPageSize() - 1) / pageable.getPageSize());
        this.pageable = pageable;
        this.paginator = paginator;
    }

    public boolean isEmpty() {
        return totalRecords == 0;
    }

    public boolean isLastPage() {
        return pageNumber == getTotalPages();
    }

    public boolean isFirstPage() {
        return pageNumber == 1;
    }

    public boolean hasNextPage() {
        return pageNumber < getTotalPages();
    }

    public boolean hasPreviousPage() {
        return pageNumber > 1;
    }

    public Iterator<EachPage<T>> iterator() {
        return new PageIterator<T>(this);
    }

    static class PageIterator<T> implements Iterator<EachPage<T>> {

        private final PageResponse<T> pageResponse;
        private PageResponse<T> current;
        private EachPage<T> page;

        PageIterator(PageResponse<T> pageResponse) {
            this.pageResponse = pageResponse;
        }

        public boolean hasNext() {
            if (current == null) {
                current = pageResponse;
            } else if (current.hasNextPage()) {
                current = current.nextPage();
            } else {
                current = null;
            }
            List<T> content = current != null ? current.getContent() : null;
            if (content != null && content.size() > 0) {
                page = new ReadonlyPage<>(content, current);
            } else {
                page = null;
            }
            return page != null;
        }

        public EachPage<T> next() {
            return page;
        }
    }

    public int getTotalPages() {
        return totalPages;
    }

    public long getTotalRecords() {
        return totalRecords;
    }

    public long getOffset() {
        return pageable.getOffset();
    }

    public int getPageSize() {
        return pageable.getPageSize();
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public PageResponse<T> setPage(int pageNumber) {
        return new PageResponseImpl<T>(pageable.withPage(pageNumber), paginator);
    }

    public PageResponse<T> lastPage() {
        int lastPage = getTotalPages();
        return isLastPage() ? this : setPage(lastPage);
    }

    public PageResponse<T> firstPage() {
        return isFirstPage() ? this : new PageResponseImpl<T>(pageable.first(), paginator);
    }

    public PageResponse<T> nextPage() {
        return hasNextPage() ? new PageResponseImpl<T>(pageable.next(), paginator) : this;
    }

    public PageResponse<T> previousPage() {
        return hasPreviousPage() ? new PageResponseImpl<T>(pageable.previousOrFirst(), paginator)
                : this;
    }

    @Override
    public List<T> getContent() {
        return paginator.list(pageable.getPageSize(), pageable.getOffset());
    }

    @Override
    public Page<T> toPage() throws Exception {
        // The page number here is one based whereas the one of Spring Data is zero based
        return new PageImpl<T>(getContent(),
                org.springframework.data.domain.PageRequest.of(getPageNumber() - 1, getPageSize()),
                totalRecords);
    }

    @Override
    public void forEachPage(Consumer<EachPage<T>> consumer) {
        Iterator<EachPage<T>> iterator = iterator();
        EachPage<T> eachPage;
        while (iterator.hasNext()) {
            eachPage = iterator.next();
            if (consumer != null) {
                consumer.accept(eachPage);
            }
        }
    }

}
