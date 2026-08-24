package com.aaax.core.response;

/**
 * Copied from qs app-core {@code com.quinsic.core.response.R}.
 */
public final class R {

    private R() {}

    public static <T> Result<T> success() {
        return new Result<>(SystemResponse.SYS0000);
    }

    public static <T> Result<T> success(Response response, T data, Pagination pagination) {
        return new Result<>(response, data, pagination);
    }

    public static <T> Result<T> success(Response response, T data) {
        return new Result<>(response, data);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(SystemResponse.SYS0000, data);
    }

    public static <T> Result<T> success(T data, Pagination pagination) {
        return new Result<>(SystemResponse.SYS0000, data, pagination);
    }

    public static <T> Result<T> fail(T data) {
        return new Result<>(SystemResponse.SYS9999, data);
    }

    public static <T> Result<T> invalid(T data) {
        return new Result<>(SystemResponse.PAM0400, data);
    }

    public static <T> Result<T> fail(Response response, T data) {
        return new Result<>(response, data);
    }

    public static <T> Result<T> error(Response response) {
        return new Result<>(response);
    }

    public static <T> Result<T> error(Response response, T data) {
        return new Result<>(response, data);
    }
}
