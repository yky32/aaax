package com.aaax.exception.response;

import com.aaax.core.response.Response;

import org.springframework.http.HttpStatus;

/**
 * Domain error codes — qs/uaa {@code exception/response/*ErrorResponse} pattern.
 */
public interface AccountErrorResponse {

    Response ACC0001 = new Response("ACC0001", "User Record not found.", HttpStatus.NOT_FOUND);
    Response ACC0002 = new Response("ACC0002", "Invalid User [status] or [password].", HttpStatus.BAD_REQUEST);
    Response ACC0003 = new Response("ACC0003", "This is recent password.", HttpStatus.BAD_REQUEST);
    Response ACC0004 = new Response("ACC0004", "User Record is invalid.", HttpStatus.CONFLICT);
    Response ACC0400 = new Response("ACC0400", "Sorry. Incorrect Parameters.", HttpStatus.BAD_REQUEST);
    Response ACC0403 = new Response("ACC0403", "Please provide password value.", HttpStatus.BAD_REQUEST);
    Response ACC0409 = new Response("ACC0409", "User Record existed.", HttpStatus.CONFLICT);
    Response ACC8400 = new Response("ACC8400", "Sorry. Forgot password was invoked.", HttpStatus.BAD_REQUEST);
}
