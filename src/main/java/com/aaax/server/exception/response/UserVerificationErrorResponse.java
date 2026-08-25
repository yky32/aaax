package com.aaax.server.exception.response;


import com.aaax.core.response.Response;
import org.springframework.http.HttpStatus;

/**
 * Example:
 * Response [error code] = new Response( [error code], [error message] );
 */
public interface UserVerificationErrorResponse {
    Response UVR0001 = new Response("UVR0001", "User Verification Record not found.", HttpStatus.NOT_FOUND);
    Response UVR0429 = new Response("UVR0429", "Too many failures for IDV..", HttpStatus.BAD_REQUEST);
}
