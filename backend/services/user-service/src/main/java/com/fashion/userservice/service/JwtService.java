package com.fashion.userservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import com.fashion.userservice.entity.User;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    // hàm trích xuất email từ token
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // hàm trích xuất yêu cầu từ token
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // hàm tạo access token sau khi token build ban đầu hết hạn
    public String generateAccessToken(User user) {
        return buildToken(user, jwtExpiration);
    }

    // hàm tạo refresh token
    public String generateRefreshToken(User user) {
        return buildToken(user, refreshExpiration);
    }

    // hàm kiểm tra token có hợp lệ không
    public boolean isTokenValid(String token, User user) {
        final String email = extractEmail(token);
        return email.equals(user.getEmail()) && !isTokenExpired(token);
    }

    // hàm kiểm tra token hết hạn không
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // hàm trích xuất thời gian hết hạn từ token
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // hàm build token ban đầu khi user login
    private String buildToken(User user, long expiration) {
        return Jwts.builder()
                .subject(user.getEmail()) // -> Lưu email/username
                // Nhồi thêm (claim) các định danh vào payload để sau này Kong giải mã ra
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
