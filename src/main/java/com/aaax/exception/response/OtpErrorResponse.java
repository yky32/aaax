package com.aaax.exception.response;

import com.aaax.core.response.Response;

import org.springframework.http.HttpStatus;

/** OTP domain codes (qs/uaa OtpErrorResponse shape). */
public interface OtpErrorResponse {

    Response OTP0001 = new Response("OTP0001", "Invalid otp session.", HttpStatus.BAD_REQUEST);
    Response OTP0002 = new Response("OTP0002", "Invalid otp.", HttpStatus.BAD_REQUEST);
    Response OTP0003 = new Response("OTP0003", "Otp is already verified / missed.", HttpStatus.BAD_REQUEST);
    Response OTP0400 = new Response("OTP0400", "Invalid otp request parameter.", HttpStatus.BAD_REQUEST);
    Response OTP0429 = new Response("OTP0429", "Invalid otp.", HttpStatus.BAD_REQUEST);
    Response OTP2001 = new Response("OTP2001", "Verified.", HttpStatus.OK);
}
