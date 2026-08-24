package com.aaax.exception.response;


import com.aaax.core.response.Response;
import org.springframework.http.HttpStatus;

/**
 * Example:
 * Response [error code] = new Response( [error code], [error message] );
 */
public interface RbacTemplateErrorResponse {
    Response RBA0001 = new Response("RBA0001", "Rbac Template Record not found.", HttpStatus.NOT_FOUND);
}
