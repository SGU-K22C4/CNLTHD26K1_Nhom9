//file này để giả lập Kong Plugin đọc header do Kong truyền xuống giảm tải cho user-service
package com.fashion.userservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.Collections;

@Component
public class KongHeaderAuthFilter extends OncePerRequestFilter {

    @Value("${security.internal.caller:chatbot-service}")
    private String trustedInternalCaller;

    @Value("${security.internal.shared-secret:local-chatbot-internal-secret}")
    private String trustedInternalSharedSecret;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // 1. Đọc các chuỗi Header do API Gateway (hoặc Kong) truyền xuống
        String userId = request.getHeader("X-User-Id");
        String userRole = request.getHeader("X-User-Role");
        String username = request.getHeader("X-Consumer-Username");
        String internalCaller = request.getHeader("X-Internal-Caller");
        String internalAuth = request.getHeader("X-Internal-Auth");

        // 2. Chặn Request nội bộ: Nếu các context Header tồn tại thì ta cấp quyền cho
        // User
        if (userId != null && userRole != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Mapping Role từ chữ thường sang Format của Spring ("ROLE_ADMIN",
            // "ROLE_CUSTOMER")
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + userRole);

            // Sinh object định danh Security MÀ KHÔNG CẦN CHỌC VÀO DATABASE (Tiết kiệm hiệu
            // suất cực lớn)
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    username, null, Collections.singletonList(authority));

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        // Internal trusted caller path for chatbot-service -> user-service.
        if (userId != null
                && trustedInternalCaller.equals(internalCaller)
                && trustedInternalSharedSecret.equals(internalAuth)
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userId, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_INTERNAL_SERVICE")));

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }
}
