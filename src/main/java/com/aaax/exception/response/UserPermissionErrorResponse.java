package com.aaax.exception.response;


import com.aaax.core.response.Response;
import org.springframework.http.HttpStatus;

/**
 * Example:
 * Response [error code] = new Response( [error code], [error message] );
 */
public interface UserPermissionErrorResponse {
    Response USP0001 = new Response("USP0001", "User Permission Record not found.", HttpStatus.NOT_FOUND);
}
