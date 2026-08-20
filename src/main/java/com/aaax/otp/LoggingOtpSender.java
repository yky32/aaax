package com.aaax.otp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default pluggable sender — logs OTP (swap for email/SMS provider later).
 */
@Component
public class LoggingOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingOtpSender.class);

    @Override
    public void send(String destination, String code) {
        log.info("AAAX OTP for {} => {}", destination, code);
    }
}
