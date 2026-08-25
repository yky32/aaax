package com.aaax.server.exception.response;

import com.aaax.core.response.Response;
import org.springframework.http.HttpStatus;

/**
 * Example:
 * Response [error code] = new Response( [error code], [error message] );
 */
public interface UserProfileErrorResponse {
    Response UPR0001 = new Response("UPR0001", "User Profile not found.", HttpStatus.NOT_FOUND);
    Response UPR0002 = new Response("UPR0002", "Too many avatars found, please upload one.", HttpStatus.BAD_REQUEST);
    Response UPR0003 = new Response("UPR0003", "Incorrect request parameters.", HttpStatus.BAD_REQUEST);
}
