package com.aaax.core.response;

import org.springframework.http.HttpStatus;

/**
 * Copied shape from qs app-core {@code com.quinsic.core.response.Response} (in-project, no private Maven).
 */
public class Response {

    private String code;
    private String message;
    private HttpStatus httpStatus;

    public Response() {}

    public Response(String code, String message) {
        this.code = code;
        this.message = message;
        this.httpStatus = HttpStatus.OK;
    }

    public Response(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }
}
