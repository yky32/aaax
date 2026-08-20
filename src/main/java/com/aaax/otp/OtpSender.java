package com.aaax.otp;

public interface OtpSender {
    void send(String destination, String code);
}
