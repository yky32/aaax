package com.aaax.exception;

import com.aaax.core.exception.BizException;

import org.springframework.http.HttpStatus;

/** Account-domain errors (thin alias over core {@link BizException}). */
public class AccountException extends BizException {

    public AccountException(HttpStatus status, String reason) {
        super(status, reason);
    }

    public static AccountException conflict(String reason) {
        return new AccountException(HttpStatus.CONFLICT, reason);
    }

    public static AccountException badRequest(String reason) {
        return new AccountException(HttpStatus.BAD_REQUEST, reason);
    }

    public static AccountException notFound(String reason) {
        return new AccountException(HttpStatus.NOT_FOUND, reason);
    }
}
