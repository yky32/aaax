package com.aaax.account;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class AccountException extends ResponseStatusException {

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
