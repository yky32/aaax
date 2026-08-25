package com.aaax.exception.response;


import com.aaax.core.response.Response;
import org.springframework.http.HttpStatus;

/**
 * Example:
 * Response [error code] = new Response( [error code], [error message] );
 */
public interface UserDeviceErrorResponse {
    Response UDV0001 = new Response("UDV0001", "User Device Record not found", HttpStatus.NOT_FOUND);
    Response UDV0002 = new Response("UDV0002", "Register User Device parameter is invalid.", HttpStatus.BAD_REQUEST);
}
