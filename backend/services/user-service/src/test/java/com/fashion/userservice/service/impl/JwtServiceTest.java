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

    

    // Một chuỗi Secret Key hợp lệ (phải đủ độ dài cho HMAC-SHA và được Base64 encode)
    // Chuỗi gốc: "fashion_secret_key_2024_microservices_architecture_top_secret"
    private final String MOCK_SECRET = Base64.getEncoder().encodeToString(
            "fashion_secret_key_2024_microservices_architecture_top_secret".getBytes()
    );

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Tiêm các giá trị @Value vào private fields
        ReflectionTestUtils.setField(jwtService, "secretKey", MOCK_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L); // 1 hour
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 604800000L); // 7 days
    }

    @Test
    @DisplayName("Tạo Access Token - Kiểm tra tính hợp lệ của Payload")
    void generateAccessToken_ShouldContainCorrectClaims() {
        // Arrange
        User user = User.builder()
                .id("user-uuid-123")
                .email("test@fashion.com")
                .role(User.Role.CUSTOMER)
                .build();

        // Act
        String token = jwtService.generateAccessToken(user);

        // Assert
        assertNotNull(token);
        assertEquals("test@fashion.com", jwtService.extractEmail(token));
        
        // Kiểm tra custom claims (userId và role)
        String extractedUserId = jwtService.extractClaim(token, claims -> claims.get("userId", String.class));
        String extractedRole = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
        
        assertEquals("user-uuid-123", extractedUserId);
        assertEquals("CUSTOMER", extractedRole);
    }

    @Test
    @DisplayName("Kiểm tra Token hợp lệ - Thành công")
    void isTokenValid_ShouldReturnTrue_ForCorrectUserAndNotExpired() {
        // Arrange
        User user = User.builder().id("1").email("valid@gmail.com").role(User.Role.ADMIN).build();
        String token = jwtService.generateAccessToken(user);

        // Act
        boolean isValid = jwtService.isTokenValid(token, user);

        // Assert
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Kiểm tra Token hợp lệ - Thất bại khi sai User")
    void isTokenValid_ShouldReturnFalse_WhenEmailDoesNotMatch() {
        // Arrange
        User user = User.builder().id("1").email("user1@gmail.com").role(User.Role.CUSTOMER).build();
        User stranger = User.builder().id("2").email("stranger@gmail.com").role(User.Role.CUSTOMER).build();
        String token = jwtService.generateAccessToken(user);

        // Act
        boolean isValid = jwtService.isTokenValid(token, stranger);

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Tạo Refresh Token - Phải có thời hạn dài hơn Access Token")
    void generateRefreshToken_ShouldHaveLongerExpiration() {
        // Arrange
        User user = User.builder().id("1").email("refresh@gmail.com").role(User.Role.CUSTOMER).build();

        // Act
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Assert
        long accessExp = jwtService.extractClaim(accessToken, claims -> claims.getExpiration().getTime());
        long refreshExp = jwtService.extractClaim(refreshToken, claims -> claims.getExpiration().getTime());

        assertTrue(refreshExp > accessExp);
    }
}