package com.fashion.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend-url:${FRONTEND_URL:http://localhost:5173}}")
    private String frontendUrl;

    @Async
    public void sendPasswordResetEmail(String toEmail, String token) {
        String resetLink = frontendUrl + "/reset-password?token=" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Reset your password");
        message.setText(
            "Hello,\n\n" +
            "You requested to reset your password. Click the link below (valid 15 minutes):\n\n" +
            resetLink + "\n\n" +
            "If you didn't request this, please ignore this email.\n\n" +
            "Fashion Store Team"
        );

        try {
            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String firstName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Welcome to Fashion Store!");
        message.setText(
            "Hi " + firstName + ",\n\n" +
            "Welcome to Fashion Store! Your account has been created successfully.\n\n" +
            "Start shopping at: " + frontendUrl + "\n\n" +
            "Fashion Store Team"
        );

        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendVerificationEmail(String toEmail, String firstName, String token) {
        String verificationLink = frontendUrl + "/verify-email?token=" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Verify your email address - Fashion Store");
        message.setText(
            "Hi " + firstName + ",\n\n" +
            "Almost there! Please verify your email address to activate your Fashion Store account.\n\n" +
            "Click the link below (valid for 24 hours):\n\n" +
            verificationLink + "\n\n" +
            "If you didn't create an account, please ignore this email.\n\n" +
            "Fashion Store Team"
        );

        try {
            mailSender.send(message);
            log.info("Verification email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage());
        }
    }
}
