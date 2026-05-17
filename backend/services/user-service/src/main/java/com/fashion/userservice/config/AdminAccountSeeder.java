package com.fashion.userservice.config;

import com.fashion.userservice.entity.User;
import com.fashion.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin-email:}")
    private String adminEmail;

    @Value("${app.bootstrap.admin-password:}")
    private String adminPassword;

    @Value("${app.bootstrap.admin-full-name:System Admin}")
    private String adminFullName;

    @Override
    public void run(String... args) {
        if (isBlank(adminEmail) || isBlank(adminPassword)) {
            log.info("Admin bootstrap skipped: app.bootstrap.admin-email/password are empty.");
            return;
        }

        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Admin bootstrap skipped: account already exists for {}", adminEmail);
            return;
        }

        User adminUser = User.builder()
                .email(adminEmail.trim())
                .password(passwordEncoder.encode(adminPassword))
                .fullName(adminFullName)
                .role(User.Role.ADMIN)
                .isEmailVerified(true)
                .build();

        userRepository.save(adminUser);
        log.info("Admin bootstrap completed for {}", adminEmail);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
