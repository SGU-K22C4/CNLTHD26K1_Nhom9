package com.fashion.userservice.service;

import com.fashion.userservice.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    private final String MOCK_SECRET = Base64.getEncoder().encodeToString(
            "fashion_secret_key_2024_microservices_architecture_top_secret".getBytes()
    );

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", MOCK_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 604800000L);
    }

    @Test
    @DisplayName("Tao Access Token - Kiem tra tinh hop le cua Payload")
    void generateAccessToken_ShouldContainCorrectClaims() {
        User user = User.builder()
                .id("user-uuid-123")
                .email("test@fashion.com")
                .role(User.Role.CUSTOMER)
                .build();

        String token = jwtService.generateAccessToken(user);

        assertNotNull(token);
        assertEquals("test@fashion.com", jwtService.extractEmail(token));

        String extractedUserId = jwtService.extractClaim(token, claims -> claims.get("userId", String.class));
        String extractedRole = jwtService.extractClaim(token, claims -> claims.get("role", String.class));

        assertEquals("user-uuid-123", extractedUserId);
        assertEquals("CUSTOMER", extractedRole);
    }

    @Test
    @DisplayName("Kiem tra Token hop le - Thanh cong")
    void isTokenValid_ShouldReturnTrue_ForCorrectUserAndNotExpired() {
        User user = User.builder().id("1").email("valid@gmail.com").role(User.Role.ADMIN).build();
        String token = jwtService.generateAccessToken(user);

        boolean isValid = jwtService.isTokenValid(token, user);

        assertTrue(isValid);
    }

    @Test
    @DisplayName("Kiem tra Token hop le - That bai khi sai User")
    void isTokenValid_ShouldReturnFalse_WhenEmailDoesNotMatch() {
        User user = User.builder().id("1").email("user1@gmail.com").role(User.Role.CUSTOMER).build();
        User stranger = User.builder().id("2").email("stranger@gmail.com").role(User.Role.CUSTOMER).build();
        String token = jwtService.generateAccessToken(user);

        boolean isValid = jwtService.isTokenValid(token, stranger);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Tao Refresh Token - Phai co thoi han dai hon Access Token")
    void generateRefreshToken_ShouldHaveLongerExpiration() {
        User user = User.builder().id("1").email("refresh@gmail.com").role(User.Role.CUSTOMER).build();

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        long accessExp = jwtService.extractClaim(accessToken, claims -> claims.getExpiration().getTime());
        long refreshExp = jwtService.extractClaim(refreshToken, claims -> claims.getExpiration().getTime());

        assertTrue(refreshExp > accessExp);
    }
}
