package com.fashion.userservice.service;

import com.fashion.userservice.dto.request.LoginRequest;
import com.fashion.userservice.dto.request.RegisterRequest;
import com.fashion.userservice.dto.response.AuthResponse;
import com.fashion.userservice.entity.RefreshToken;
import com.fashion.userservice.entity.User;
import com.fashion.userservice.exception.EmailAlreadyExistsException;
import com.fashion.userservice.exception.InvalidCredentialsException;
import com.fashion.userservice.repository.EmailVerificationTokenRepository;
import com.fashion.userservice.repository.PasswordResetTokenRepository;
import com.fashion.userservice.repository.RefreshTokenRepository;
import com.fashion.userservice.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private MailService mailService;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Đăng ký thành công - Không yêu cầu xác thực email")
    void register_Success_NoVerification() {
        // Arrange
        ReflectionTestUtils.setField(authService, "emailVerificationRequired", false);
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@gmail.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_pass");

        // Act
        AuthResponse response = authService.register(request);

        // Assert
        assertNotNull(response);
        verify(userRepository).save(argThat(user -> user.isEmailVerified()));
        verify(emailVerificationTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đăng ký thất bại - Email đã tồn tại")
    void register_Fail_EmailExists() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("exists@gmail.com");
        when(userRepository.existsByEmail("exists@gmail.com")).thenReturn(true);

        // Act & Assert
        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(request));
    }

    @Test
@DisplayName("Login thành công - Trả về Access Token")
void login_Success() {
    // 1. Arrange
    String email = "test@gmail.com";
    String password = "password123";
    LoginRequest loginRequest = new LoginRequest(email, password);
    
    // Sử dụng một chuỗi BCrypt chuẩn để vượt qua kiểm tra isLegacyPlainPassword
    String encodedPassword = "$2a$10$8S8B6wS6pS8B6wS6pS8B6uYvjG9.3Z4l5m6n7o8p9q0r1s2t3u4v5"; 

    User mockUser = User.builder()
            .id("user-123")
            .email(email)
            .password(encodedPassword) // Mật khẩu đã mã hóa
            .isEmailVerified(true)
            .role(User.Role.CUSTOMER)
            .fullName("Fashion User")
            .build();

    // Mock các hành vi
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));
    
    // Mock authenticationManager: Trả về null hoặc một Authentication object giả đều được
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(null); 
    
    when(jwtService.generateAccessToken(any(User.class))).thenReturn("mock-access-token");
    when(jwtService.generateRefreshToken(any(User.class))).thenReturn("mock-refresh-token");

    // 2. Act
    AuthResponse response = authService.login(loginRequest);

    // 3. Assert
    assertNotNull(response);
    assertEquals("mock-access-token", response.getAccessToken());
    assertEquals(email, response.getEmail());
    verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
}

    @Test
    @DisplayName("Đăng nhập thất bại - Tài khoản chưa xác thực email")
    void login_Fail_NotVerified() {
        // Arrange
        ReflectionTestUtils.setField(authService, "emailVerificationRequired", true);
        LoginRequest request = new LoginRequest("unverified@gmail.com", "password");
        User user = User.builder().email("unverified@gmail.com").isEmailVerified(false).build();

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

        // Act & Assert
        assertThrows(DisabledException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Migration mật khẩu - Đăng nhập với mật khẩu cũ (Plain Text)")
    void login_Success_WithLegacyPasswordMigration() {
        // Arrange
        LoginRequest request = new LoginRequest("olduser@gmail.com", "plain123");
        User user = User.builder()
                .email("olduser@gmail.com")
                .password("plain123") // Mật khẩu chưa băm
                .role(User.Role.CUSTOMER)
                .build();

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("plain123")).thenReturn("$2a$10$new_bcrypt_hash");

        // Act
        authService.login(request);

        // Assert
        verify(userRepository).save(argThat(u -> u.getPassword().equals("$2a$10$new_bcrypt_hash")));
        verify(authenticationManager, never()).authenticate(any()); // Không dùng AuthManager vì pass cũ chưa băm
    }

    @Test
    @DisplayName("Refresh Token thành công")
    void refreshToken_Success() {
        // Arrange
        User user = User.builder().email("user@gmail.com").fullName("Test User").role(User.Role.CUSTOMER).build();
        RefreshToken stored = RefreshToken.builder()
                .token("valid_refresh")
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken("valid_refresh")).thenReturn(Optional.of(stored));
        when(jwtService.generateAccessToken(user)).thenReturn("new_access_token");

        // Act
        AuthResponse response = authService.refreshToken("valid_refresh");

        // Assert
        assertEquals("new_access_token", response.getAccessToken());
        assertEquals("valid_refresh", response.getRefreshToken());
    }

    @Test
    @DisplayName("Quên mật khẩu - Gửi mail thành công")
    void forgotPassword_Success() {
        // Arrange
        User user = User.builder().id("user-123").email("target@gmail.com").build();
        when(userRepository.findByEmail("target@gmail.com")).thenReturn(Optional.of(user));

        // Act
        authService.forgotPassword(new com.fashion.userservice.dto.request.ForgotPasswordRequest("target@gmail.com"));

        // Assert
        verify(passwordResetTokenRepository).deleteAllByUserId("user-123");
        verify(passwordResetTokenRepository).save(any());
        verify(mailService).sendPasswordResetEmail(eq("target@gmail.com"), anyString());
    }
}