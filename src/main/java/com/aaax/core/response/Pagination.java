package com.aaax.core.response;

/**
 * Minimal pagination placeholder (app-core shape). Filled when list APIs need it.
 */
public class Pagination {

    private long page;
    private long size;
    private long total;

    public Pagination() {}

    public Pagination(long page, long size, long total) {
        this.page = page;
        this.size = size;
        this.total = total;
    }

    public long getPage() {
        return page;
    }

    public void setPage(long page) {
        this.page = page;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}
