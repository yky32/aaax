package com.aaax.core.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * Copied shape from qs app-core {@code com.quinsic.core.response.Result}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {

    @JsonUnwrapped
    private Response response;
    private T data;
    private String requestId;
    private String tenantKey;
    private Pagination pagination;

    public Result() {}

    public Result(Response response) {
        this.response = response;
    }

    public Result(Response response, T data) {
        this.response = response;
        this.data = data;
    }

    public Result(Response response, T data, Pagination pagination) {
        this.response = response;
        this.data = data;
        this.pagination = pagination;
    }

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getTenantKey() {
        return tenantKey;
    }

    public void setTenantKey(String tenantKey) {
        this.tenantKey = tenantKey;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }
}
