package com.aaax.spi.otp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default OTP channel — prints to logs (local / CI).
 */
@Component
@ConditionalOnProperty(name = "aaax.otp.channel", havingValue = "console", matchIfMissing = true)
public class LoggingOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingOtpSender.class);

    @Override
    public void send(String destination, String code) {
        log.info("AAAX OTP for {} => {}", destination, code);
    }
}
