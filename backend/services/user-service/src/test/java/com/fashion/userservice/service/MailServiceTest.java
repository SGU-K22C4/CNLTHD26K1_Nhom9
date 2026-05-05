package com.fashion.userservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private MailService mailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mailService, "fromEmail", "noreply@fashion.com");
        ReflectionTestUtils.setField(mailService, "frontendUrl", "http://fashion-store.com");
    }

    @Test
    @DisplayName("Gui mail Reset Password - Phai chua dung link va email nhan")
    void sendPasswordResetEmail_ShouldSendCorrectMessage() {
        String toEmail = "user@gmail.com";
        String token = "reset-token-123";

        mailService.sendPasswordResetEmail(toEmail, token);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertEquals(toEmail, Objects.requireNonNull(sentMessage.getTo())[0]);
        assertEquals("Reset your password", sentMessage.getSubject());
        assertTrue(Objects.requireNonNull(sentMessage.getText()).contains("http://fashion-store.com/reset-password?token=" + token));
    }

    @Test
    @DisplayName("Gui mail xac thuc Email - Phai chua link verify")
    void sendVerificationEmail_ShouldSendCorrectMessage() {
        String toEmail = "newuser@gmail.com";
        String firstName = "John";
        String token = "verify-token-456";

        mailService.sendVerificationEmail(toEmail, firstName, token);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertEquals("Verify your email address - Fashion Store", sentMessage.getSubject());
        assertTrue(Objects.requireNonNull(sentMessage.getText()).contains(firstName));
        assertTrue(sentMessage.getText().contains("http://fashion-store.com/verify-email?token=" + token));
    }

    @Test
    @DisplayName("Xu ly loi khi mail server gap su co")
    void sendWelcomeEmail_ShouldHandleExceptionGracefully() {
        doThrow(new RuntimeException("SMTP Server Down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> mailService.sendWelcomeEmail("test@gmail.com", "John"));
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
