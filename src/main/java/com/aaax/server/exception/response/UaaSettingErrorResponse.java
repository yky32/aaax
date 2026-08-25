package com.aaax.server.exception.response;


import com.aaax.core.response.Response;
import org.springframework.http.HttpStatus;

/**
 * Example:
 * Response [error code] = new Response( [error code], [error message] );
 */
public interface UaaSettingErrorResponse {
    Response UAS0001 = new Response("UAS0001", "No this client.", HttpStatus.NOT_FOUND);
}
