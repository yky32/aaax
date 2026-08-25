package com.aaax.server.exception.response;


import com.aaax.core.response.Response;
import org.springframework.http.HttpStatus;

/**
 * Example:
 * Response [error code] = new Response( [error code], [error message] );
 */
public interface UserPreferenceErrorResponse {
    Response UPN0001 = new Response("UPN0001", "This User Preference Config Key are not available yet.", HttpStatus.BAD_REQUEST);
    Response UPN0002 = new Response("UPN0002", "This User Preference Record not found.", HttpStatus.NOT_FOUND);
}
