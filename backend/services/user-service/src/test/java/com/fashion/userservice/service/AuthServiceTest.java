package com.fashion.userservice.service;

import com.fashion.userservice.dto.request.ForgotPasswordRequest;
import com.fashion.userservice.dto.request.LoginRequest;
import com.fashion.userservice.dto.request.RegisterRequest;
import com.fashion.userservice.dto.response.AuthResponse;
import com.fashion.userservice.entity.RefreshToken;
import com.fashion.userservice.entity.User;
import com.fashion.userservice.exception.EmailAlreadyExistsException;
import com.fashion.userservice.repository.AddressRepository;
import com.fashion.userservice.repository.EmailVerificationTokenRepository;
import com.fashion.userservice.repository.PasswordResetTokenRepository;
import com.fashion.userservice.repository.RefreshTokenRepository;
import com.fashion.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    private AuthService authService;
    private StubMailService mailService;

    @BeforeEach
    void setUp() {
        JwtService jwtService = new JwtService() {
            @Override
            public String generateAccessToken(User user) {
                return "mock-access-token";
            }

            @Override
            public String generateRefreshToken(User user) {
                return "mock-refresh-token";
            }
        };

        mailService = new StubMailService();

        authService = new AuthService(
                userRepository,
                addressRepository,
                refreshTokenRepository,
                emailVerificationTokenRepository,
                passwordResetTokenRepository,
                passwordEncoder,
                jwtService,
                mailService,
                authenticationManager
        );
    }

    @Test
    void should_RegisterUserWithoutVerification_When_EmailVerificationIsDisabled() {
        ReflectionTestUtils.setField(authService, "emailVerificationRequired", false);
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@gmail.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_pass");

        AuthResponse response = authService.register(request);

        assertEquals("test@gmail.com", response.getEmail());
        verify(userRepository).save(any(User.class));
        verify(emailVerificationTokenRepository, never()).save(any());
    }

    @Test
    void should_ThrowEmailAlreadyExistsException_When_RegisteringExistingEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("exists@gmail.com");
        when(userRepository.existsByEmail("exists@gmail.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(request));
    }

    @Test
    void should_ReturnTokens_When_LoginSucceedsWithEncodedPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@gmail.com");
        request.setPassword("password123");

        User user = User.builder()
                .id("user-123")
                .email("test@gmail.com")
                .password("$2a$10$8S8B6wS6pS8B6wS6pS8B6uYvjG9.3Z4l5m6n7o8p9q0r1s2t3u4v5")
                .isEmailVerified(true)
                .role(User.Role.CUSTOMER)
                .fullName("Fashion User")
                .build();

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);

        AuthResponse response = authService.login(request);

        assertEquals("mock-access-token", response.getAccessToken());
        assertEquals("mock-refresh-token", response.getRefreshToken());
        assertEquals("test@gmail.com", response.getEmail());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void should_ThrowDisabledException_When_LoginRequiresVerifiedEmail() {
        ReflectionTestUtils.setField(authService, "emailVerificationRequired", true);
        LoginRequest request = new LoginRequest();
        request.setEmail("unverified@gmail.com");
        request.setPassword("password");
        User user = User.builder().email("unverified@gmail.com").isEmailVerified(false).build();

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

        assertThrows(DisabledException.class, () -> authService.login(request));
    }

    @Test
    void should_MigrateLegacyPassword_When_LoginUsesPlainTextPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("olduser@gmail.com");
        request.setPassword("plain123");
        User user = User.builder()
                .email("olduser@gmail.com")
                .password("plain123")
                .role(User.Role.CUSTOMER)
                .build();

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("plain123")).thenReturn("$2a$10$new_bcrypt_hash");

        authService.login(request);

        verify(userRepository).save(any(User.class));
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void should_ReturnNewAccessToken_When_RefreshTokenIsValid() {
        User user = User.builder()
                .email("user@gmail.com")
                .fullName("Test User")
                .role(User.Role.CUSTOMER)
                .build();
        RefreshToken stored = RefreshToken.builder()
                .token("valid_refresh")
                .user(user)
                .expiresAt(LocalDateTime.of(2099, 1, 1, 0, 0))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken("valid_refresh")).thenReturn(Optional.of(stored));

        AuthResponse response = authService.refreshToken("valid_refresh");

        assertEquals("mock-access-token", response.getAccessToken());
        assertEquals("valid_refresh", response.getRefreshToken());
    }

    @Test
    void should_SaveResetTokenAndSendEmail_When_ForgotPasswordMatchesExistingUser() {
        User user = User.builder().id("user-123").email("target@gmail.com").build();
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("target@gmail.com");

        when(userRepository.findByEmail("target@gmail.com")).thenReturn(Optional.of(user));

        authService.forgotPassword(request);

        verify(passwordResetTokenRepository).deleteAllByUserId("user-123");
        verify(passwordResetTokenRepository).save(any());
        assertEquals("target@gmail.com", mailService.lastPasswordResetEmail);
        assertEquals(true, mailService.passwordResetSent);
    }

    private static class StubMailService extends MailService {
        private boolean passwordResetSent;
        private String lastPasswordResetEmail;

        StubMailService() {
            super((JavaMailSender) null);
        }

        @Override
        public void sendPasswordResetEmail(String toEmail, String token) {
            this.passwordResetSent = true;
            this.lastPasswordResetEmail = toEmail;
        }
    }
}
