package com.rest;

import spark.Request;

public class Page {
    private Integer pageSize;
    private Integer page;
    private Long offset;
    private Long count = 0L;
    private Long total;

    public boolean write() {
        return count >= offset;
    }

    public boolean finish() {
        return count >= total;
    }

    public Page(Request request) {
        this.page = Integer.valueOf(request.queryParamOrDefault("page", "1"));
        this.pageSize = Integer.valueOf(request.queryParamOrDefault("page_size", "10"));
        this.offset = (long) ((page - 1) * pageSize);
        this.total = offset + pageSize;
    }

    public void increase() {
        count++;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }
}
