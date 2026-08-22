package com.aaax.spi.otp;

import com.aaax.entity.dto.event.OtpDispatchEventDto;

/**
 * Pluggable SMS delivery. AAAX does <strong>not</strong> embed Twilio/etc.
 * Callers implement via webhook URL or Kafka consumer → own notification-service.
 */
public interface SmsSender {
    void sendSms(String e164OrLocalPhone, String messageBody, OtpDispatchEventDto event);
}
