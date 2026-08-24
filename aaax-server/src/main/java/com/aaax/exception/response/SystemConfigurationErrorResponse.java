package com.aaax.exception.response;


import com.aaax.core.response.Response;
import org.springframework.http.HttpStatus;

/**
 * Example:
 * Response [error code] = new Response( [error code], [error message] );
 */
public interface SystemConfigurationErrorResponse {
    Response SYC0001 = new Response("SYC0001", "System Configuration Record not found.", HttpStatus.NOT_FOUND);
    Response SYC0002 = new Response("SYC0002", "System Configuration already exists for target+scope.", HttpStatus.CONFLICT);
}
