package com.aaax.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Business / domain error with stable HTTP status (AAAX core — not Quinsic BizException dump).
 * Prefer this over ad-hoc {@link ResponseStatusException} in use cases.
 */
public class BizException extends ResponseStatusException {

    private final String code;

    public BizException(HttpStatus status, String code, String reason) {
        super(status, reason);
        this.code = code == null || code.isBlank() ? status.name() : code;
    }

    public BizException(HttpStatus status, String reason) {
        this(status, null, reason);
    }

    public String getCode() {
        return code;
    }

    public static BizException badRequest(String reason) {
        return new BizException(HttpStatus.BAD_REQUEST, "bad_request", reason);
    }

    public static BizException badRequest(String code, String reason) {
        return new BizException(HttpStatus.BAD_REQUEST, code, reason);
    }

    public static BizException notFound(String reason) {
        return new BizException(HttpStatus.NOT_FOUND, "not_found", reason);
    }

    public static BizException conflict(String reason) {
        return new BizException(HttpStatus.CONFLICT, "conflict", reason);
    }

    public static BizException unauthorized(String reason) {
        return new BizException(HttpStatus.UNAUTHORIZED, "unauthorized", reason);
    }

    public static BizException forbidden(String reason) {
        return new BizException(HttpStatus.FORBIDDEN, "forbidden", reason);
    }
}
