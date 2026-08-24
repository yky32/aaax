package com.aaax.exception.response;

import com.aaax.core.response.Response;

import org.springframework.http.HttpStatus;

/** OAuth client admin codes (qs/uaa ClientErrorResponse shape). */
public interface ClientErrorResponse {

    Response CLT0001 = new Response("CLT0001", "Invalid client id.", HttpStatus.BAD_REQUEST);
    Response CLT0002 = new Response("CLT0002", "Client not found.", HttpStatus.NOT_FOUND);
    Response CLT0409 = new Response("CLT0409", "client_id already exists.", HttpStatus.CONFLICT);
}
