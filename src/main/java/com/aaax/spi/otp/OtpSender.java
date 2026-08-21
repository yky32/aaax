package com.aaax.spi.otp;

public interface OtpSender {
    void send(String destination, String code);
}
