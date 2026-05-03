package com.fashion.userservice.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class MailConfig {

    // Cấu hình mail được load từ application.yml qua Spring Boot auto-config.
    // Bean này chỉ cần override nếu cần custom thêm properties.
    // Mặc định Spring Boot tự tạo JavaMailSender dựa trên spring.mail.* trong yml.
}
