package com.aaax.server.exception.response;


import com.aaax.core.response.Response;
import org.springframework.http.HttpStatus;

/**
 * Example:
 * Response [error code] = new Response( [error code], [error message] );
 */
public interface ClientErrorResponse {
    Response CLT0001 = new Response("CLT0001", "Invalid client id.", HttpStatus.BAD_REQUEST);
}