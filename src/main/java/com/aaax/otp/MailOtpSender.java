package com.aaax.otp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * SMTP OTP sender. If mail host is empty, falls back to log (still boots).
 */
@Component
@ConditionalOnProperty(name = "aaax.otp.channel", havingValue = "mail")
public class MailOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(MailOtpSender.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final String subject;
    private final String host;

    public MailOtpSender(
            JavaMailSender mailSender,
            @Value("${aaax.otp.mail.from:noreply@aaax.local}") String from,
            @Value("${aaax.otp.mail.subject:Your AAAX code}") String subject,
            @Value("${spring.mail.host:}") String host) {
        this.mailSender = mailSender;
        this.from = from;
        this.subject = subject;
        this.host = host;
    }

    @Override
    public void send(String destination, String code) {
        if (!StringUtils.hasText(host) || !destination.contains("@")) {
            log.warn("Mail OTP fallback to console (host empty or destination not email): {} => {}", destination, code);
            log.info("AAAX OTP for {} => {}", destination, code);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(destination);
        message.setSubject(subject);
        message.setText("Your AAAX one-time code is: " + code + "\n\nIt expires shortly. If you did not request this, ignore.");
        mailSender.send(message);
        log.info("AAAX OTP mailed to {}", destination);
    }
}
