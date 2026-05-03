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
        // Tiêm các giá trị cấu hình từ application.yml
        ReflectionTestUtils.setField(mailService, "fromEmail", "noreply@fashion.com");
        ReflectionTestUtils.setField(mailService, "frontendUrl", "http://fashion-store.com");
    }

    @Test
    @DisplayName("Gửi mail Reset Password - Phải chứa đúng link và email nhận")
    void sendPasswordResetEmail_ShouldSendCorrectMessage() {
        // Arrange
        String toEmail = "user@gmail.com";
        String token = "reset-token-123";

        // Act
        mailService.sendPasswordResetEmail(toEmail, token);

        // Assert
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertEquals(toEmail, Objects.requireNonNull(sentMessage.getTo())[0]);
        assertEquals("Reset your password", sentMessage.getSubject());
        assertTrue(Objects.requireNonNull(sentMessage.getText()).contains("http://fashion-store.com/reset-password?token=" + token));
    }

    @Test
    @DisplayName("Gửi mail xác thực Email - Phải chứa link verify")
    void sendVerificationEmail_ShouldSendCorrectMessage() {
        // Arrange
        String toEmail = "newuser@gmail.com";
        String firstName = "John";
        String token = "verify-token-456";

        // Act
        mailService.sendVerificationEmail(toEmail, firstName, token);

        // Assert
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertEquals("Verify your email address - Fashion Store", sentMessage.getSubject());
        assertTrue(Objects.requireNonNull(sentMessage.getText()).contains(firstName));
        assertTrue(sentMessage.getText().contains("http://fashion-store.com/verify-email?token=" + token));
    }

    @Test
    @DisplayName("Xử lý lỗi khi mail server gặp sự cố")
    void sendWelcomeEmail_ShouldHandleExceptionGracefully() {
        // Arrange
        doThrow(new RuntimeException("SMTP Server Down")).when(mailSender).send(any(SimpleMailMessage.class));

        // Act & Assert
        // Vì hàm này void và có try-catch nên không throw exception ra ngoài,
        // chúng ta kiểm tra xem nó có log lỗi (thông qua việc verify code chạy qua catch)
        assertDoesNotThrow(() -> mailService.sendWelcomeEmail("test@gmail.com", "John"));
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}