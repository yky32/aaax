package com.aaax.exception.response;

import com.aaax.core.response.Response;

import org.springframework.http.HttpStatus;

/** Social / federation domain codes. */
public interface SocialErrorResponse {

    Response SOC0001 = new Response("SOC0001", "Social identity already linked to another user.", HttpStatus.CONFLICT);
    Response SOC0002 = new Response("SOC0002", "Social provider is not linked.", HttpStatus.BAD_REQUEST);
    Response SOC0003 = new Response("SOC0003", "Unknown social provider.", HttpStatus.BAD_REQUEST);
    Response SOC0004 = new Response("SOC0004", "Provider subject / id is required.", HttpStatus.BAD_REQUEST);
    Response SOC0005 = new Response("SOC0005", "Cannot unlink last login method.", HttpStatus.BAD_REQUEST);
    Response SOC0006 = new Response("SOC0006", "Account already linked to a different identity.", HttpStatus.CONFLICT);
    Response SOC0401 = new Response("SOC0401", "3rd party login failed.", HttpStatus.UNAUTHORIZED);
}
