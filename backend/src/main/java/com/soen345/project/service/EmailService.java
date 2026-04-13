package com.soen345.project.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender emailSender;

    @Value("${resend.enabled:false}")
    private boolean resendEnabled;

    @Value("${resend.api.key:}")
    private String resendApiKey;

    @Value("${resend.from:onboarding@resend.dev}")
    private String resendFrom;

    public void sendVerificationEmail(String to, String subject, String text) throws MessagingException {
        if (resendEnabled && resendApiKey != null && !resendApiKey.isBlank()) {
            sendViaResend(to, subject, text);
            return;
        }
        if (emailSender == null) {
            throw new MessagingException("Mail not configured: enable Resend (RESEND_ENABLED=true) or Gmail SMTP.");
        }
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text, false);
        emailSender.send(message);
    }

    private void sendViaResend(String to, String subject, String body) throws MessagingException {
        try {
            RestClient.create()
                    .post()
                    .uri("https://api.resend.com/emails")
                    .header("Authorization", "Bearer " + resendApiKey)
                    .header("Content-Type", "application/json")
                    .body(Map.of(
                            "from", resendFrom,
                            "to", List.of(to),
                            "subject", subject,
                            "text", body))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            throw new MessagingException("Resend send failed: " + e.getMessage(), e);
        }
    }
}
