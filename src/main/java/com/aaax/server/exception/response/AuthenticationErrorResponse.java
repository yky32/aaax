package com.aaax.server.exception.response;


import com.aaax.core.response.Response;
import org.springframework.http.HttpStatus;

/**
 * Example:
 * Response [error code] = new Response( [error code], [error message] );
 */
public interface AuthenticationErrorResponse {
    Response ATH0001 = new Response("ATH0001", "Authentication record not found.", HttpStatus.NOT_FOUND);
    /** Provider identity already linked to another UAA user. */
    Response ATH0002 = new Response("ATH0002", "Authentication identity is already linked to another account.", HttpStatus.CONFLICT);
    /** Unlink not allowed (e.g. last method, or email ACO protected). */
    Response ATH0003 = new Response("ATH0003", "Cannot unlink this authentication method.", HttpStatus.BAD_REQUEST);
    /** Missing/invalid link payload or unsupported provider. */
    Response ATH0004 = new Response("ATH0004", "Invalid linked-authentication request.", HttpStatus.BAD_REQUEST);
}
