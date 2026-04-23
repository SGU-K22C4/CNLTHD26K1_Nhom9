package com.fashion.userservice.service;

import com.fashion.userservice.dto.request.ForgotPasswordRequest;
import com.fashion.userservice.dto.request.LoginRequest;
import com.fashion.userservice.dto.request.RegisterRequest;
import com.fashion.userservice.dto.request.ResetPasswordRequest;
import com.fashion.userservice.dto.response.AuthResponse;
import com.fashion.userservice.entity.EmailVerificationToken;
import com.fashion.userservice.entity.PasswordResetToken;
import com.fashion.userservice.entity.RefreshToken;
import com.fashion.userservice.entity.User;
import com.fashion.userservice.exception.EmailAlreadyExistsException;
import com.fashion.userservice.exception.InvalidCredentialsException;
import com.fashion.userservice.exception.ResourceNotFoundException;
import com.fashion.userservice.exception.TokenExpiredException;
import com.fashion.userservice.repository.EmailVerificationTokenRepository;
import com.fashion.userservice.repository.PasswordResetTokenRepository;
import com.fashion.userservice.repository.RefreshTokenRepository;
import com.fashion.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final long REFRESH_TOKEN_DAYS = 7;
    private static final long RESET_TOKEN_MINUTES = 15;
    private static final Pattern BCRYPT_HASH_PATTERN = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MailService mailService;
    private final AuthenticationManager authenticationManager;
    @Value("${app.auth.email-verification-required:false}")
    private boolean emailVerificationRequired;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .gender(request.getGender())
                .avatar(request.getAvatar())
                .role(User.Role.CUSTOMER)
                .isEmailVerified(!emailVerificationRequired)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        if (emailVerificationRequired) {
            String token = UUID.randomUUID().toString();
            EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                    .userId(user.getId())
                    .token(token)
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .createdAt(LocalDateTime.now())
                    .build();
            emailVerificationTokenRepository.save(verificationToken);
            mailService.sendVerificationEmail(user.getEmail(), user.getFullName(), token);
        }

        String role = user.getRole() != null ? user.getRole().name() : User.Role.CUSTOMER.name();
        return AuthResponse.builder()
                .email(user.getEmail())
                .firstName(user.getFullName())
                .role(role)
                .build();
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        if (!emailVerificationRequired) {
            return;
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Email chua duoc dang ky. Vui long dang ky tai khoan truoc!"));

        if (user.isEmailVerified()) {
            throw new IllegalStateException("Email da duoc xac thuc. Ban co the dang nhap ngay!");
        }

        emailVerificationTokenRepository.deleteAllByUserId(user.getId());

        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .userId(user.getId())
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .createdAt(LocalDateTime.now())
                .build();
        emailVerificationTokenRepository.save(verificationToken);
        mailService.sendVerificationEmail(user.getEmail(), user.getFullName(), token);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (emailVerificationRequired && !user.isEmailVerified()) {
            throw new DisabledException("Tai khoan chua duoc kich hoat. Vui long kiem tra email!");
        }

        // Backward compatibility for databases that still keep old plain-text passwords.
        // First successful login migrates the password to BCrypt.
        if (isLegacyPlainPassword(user.getPassword())) {
            if (!user.getPassword().equals(request.getPassword())) {
                throw new InvalidCredentialsException("Invalid email or password");
            }
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            userRepository.save(user);
        } else {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = saveRefreshToken(user, jwtService.generateRefreshToken(user));

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .firstName(user.getFullName())
                .lastName("")
                .role(user.getRole().name())
                .build();
    }

    @Transactional
    public AuthResponse refreshToken(String rawRefreshToken) {
        RefreshToken stored = refreshTokenRepository.findByToken(rawRefreshToken)
                .orElseThrow(() -> new TokenExpiredException("Refresh token not found"));

        if (stored.isRevoked() || stored.isExpired()) {
            throw new TokenExpiredException("Refresh token expired or revoked");
        }

        User user = stored.getUser();
        String newAccessToken = jwtService.generateAccessToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(rawRefreshToken)
                .email(user.getEmail())
                .firstName(user.getFullName())
                .lastName("")
                .role(user.getRole().name())
                .build();
    }

    @Transactional
    public void logout(String userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null) {
            return;
        }

        passwordResetTokenRepository.deleteAllByUserId(user.getId());

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .userId(user.getId())
                .token(token)
                .expiresAt(LocalDateTime.now().plusMinutes(RESET_TOKEN_MINUTES))
                .build();
        passwordResetTokenRepository.save(resetToken);
        mailService.sendPasswordResetEmail(user.getEmail(), token);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new TokenExpiredException("Invalid reset token"));

        if (resetToken.isUsed() || resetToken.isExpired()) {
            throw new TokenExpiredException("Reset token expired or already used");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        refreshTokenRepository.revokeAllByUserId(user.getId());
    }

    @Transactional
    public void verifyEmail(String token) {
        if (!emailVerificationRequired) {
            return;
        }

        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenExpiredException("Invalid verification token"));

        if (verificationToken.isExpired()) {
            throw new TokenExpiredException("Verification token has expired");
        }

        User user = userRepository.findById(verificationToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setEmailVerified(true);
        userRepository.save(user);

        emailVerificationTokenRepository.deleteAllByUserId(user.getId());
    }

    private String saveRefreshToken(User user, String rawToken) {
        RefreshToken rt = RefreshToken.builder()
                .user(user)
                .token(rawToken)
                .expiresAt(LocalDateTime.now().plusDays(REFRESH_TOKEN_DAYS))
                .build();
        refreshTokenRepository.save(rt);
        return rawToken;
    }

    private boolean isLegacyPlainPassword(String storedPassword) {
        return storedPassword != null && !BCRYPT_HASH_PATTERN.matcher(storedPassword).matches();
    }
}
