package com.aaax.server.exception.response;


import com.aaax.core.response.Response;
import org.springframework.http.HttpStatus;

/**
 * Example:
 * Response [error code] = new Response( [error code], [error message] );
 */
public interface AaaxSettingErrorResponse {
    Response AAS0001 = new Response("AAS0001", "No this client.", HttpStatus.NOT_FOUND);
}
