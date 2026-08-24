package com.aaax.exception.response;


import com.aaax.core.response.Response;
import org.springframework.http.HttpStatus;

/**
 * Example:
 * Response [error code] = new Response( [error code], [error message] );
 */
public interface UseRegistrationErrorResponse {
    Response URG0001 = new Response("URG0001", "Post User Registration failed.", HttpStatus.BAD_REQUEST);
}
