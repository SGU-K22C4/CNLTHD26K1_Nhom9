package com.fashion.userservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
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
    public String generateAccessToken(UserDetails userDetails) {
        return buildToken(userDetails, jwtExpiration);
    }

    // hàm tạo refresh token
    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(userDetails, refreshExpiration);
    }

    // hàm kiểm tra token có hợp lệ không
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
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
    private String buildToken(UserDetails userDetails, long expiration) {
        // 1. Ép kiểu UserDetails về Entity User để lấy bổ sung các trường ta tự định
        // nghĩa
        com.fashion.userservice.entity.User user = (com.fashion.userservice.entity.User) userDetails;

        return Jwts.builder()
                .subject(userDetails.getUsername()) // -> Lưu email/username
                // 2. Nhồi thêm (claim) các định danh vào payload để sau này Kong giải mã ra
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
