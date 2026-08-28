package com.aaax.server.exception.response;

import com.aaax.core.response.Response;
import org.springframework.http.HttpStatus;

/**
 * Example:
 * Response [error code] = new Response( [error code], [error message] );
 */
public interface AaaxErrorResponse {
    Response AAAX0001 = new Response("AAAX0001", "User Record not found.", HttpStatus.NOT_FOUND);
    Response AAAX0002 = new Response("AAAX0002", "Invalid User [status] or [password].", HttpStatus.BAD_REQUEST);
    Response AAAX0003 = new Response("AAAX0003", "This is recent password.", HttpStatus.BAD_REQUEST);
    Response AAAX0004 = new Response("AAAX0004", "User Record is invalid.", HttpStatus.CONFLICT);
    Response AAAX0400 = new Response("AAAX0400", "Sorry. Incorrect Verification state.", HttpStatus.BAD_REQUEST);
    Response AAAX4400 = new Response("AAAX4400", "Sorry. Incorrect Parameters.", HttpStatus.BAD_REQUEST);
    Response AAAX0401 = new Response("AAAX0401", "3rd party login failed.", HttpStatus.UNAUTHORIZED);
    Response AAAX0403 = new Response("AAAX0403", "Please provide password value.", HttpStatus.BAD_REQUEST);
    Response AAAX0409 = new Response("AAAX0409", "User Record existed.", HttpStatus.CONFLICT);

    Response AAAX8400 = new Response("AAAX8400", "Sorry. Forgot password was invoked.", HttpStatus.BAD_REQUEST);
    Response AAAX8420 = new Response("AAAX8420", "Sorry. Forgot password OTP was verified.", HttpStatus.BAD_REQUEST);
}
