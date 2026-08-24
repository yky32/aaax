package com.aaax.exception;

import com.aaax.core.exception.BizException;
import com.aaax.exception.response.AccountErrorResponse;

/**
 * Thin helpers — prefer {@code throw new BizException(AccountErrorResponse.ACC…)} at call sites.
 * Kept for gradual migration from old static factories.
 */
public final class AccountException {

    private AccountException() {}

    public static BizException notFound(String detail) {
        return new BizException(AccountErrorResponse.ACC0001, detail);
    }

    public static BizException badRequest(String detail) {
        return new BizException(AccountErrorResponse.ACC0400, detail);
    }

    public static BizException conflict(String detail) {
        return new BizException(AccountErrorResponse.ACC0409, detail);
    }
}
