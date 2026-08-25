package com.aaax.exception.response;


import com.aaax.core.response.Response;
import org.springframework.http.HttpStatus;

/**
 * Example:
 * Response [error code] = new Response( [error code], [error message] );
 */
public interface UserRouteErrorResponse {
    Response USR0001 = new Response("USR0001", "User Route Record not found.", HttpStatus.NOT_FOUND);
    Response USR0002 = new Response("USR0002", "User Route Not Support multiple Trr.", HttpStatus.BAD_REQUEST);
}
