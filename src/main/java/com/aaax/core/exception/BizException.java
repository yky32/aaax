package com.aaax.core.exception;

import java.util.Map;
import java.util.Objects;

import com.aaax.core.response.Response;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Copied from qs app-core {@code com.quinsic.core.exception.BizException}.
 *
 * <pre>
 * throw new BizException(SystemResponse.PAM0400, "detail…");
 * throw new BizException(AccountErrorResponse.ACC0001);
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BizException extends RuntimeException {

    private Response response;
    private Object data;

    public BizException(Response response) {
        super(response != null ? response.getMessage() : null);
        this.response = response;
    }

    public BizException(Response response, String message) {
        super(message);
        this.response = response;
        this.data = Map.of("detail", message);
    }

    public <T, U> BizException(Response response, Map<T, U> map) {
        super(response != null ? response.getMessage() : null);
        this.response = response;
        this.data = map;
    }

    public <T> BizException(Response response, T data) {
        super(response != null ? response.getMessage() : null);
        this.response = response;
        if (data instanceof String s) {
            this.data = Map.of("detail", s);
        } else {
            this.data = Objects.requireNonNullElseGet(data, Map::of);
        }
    }

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
