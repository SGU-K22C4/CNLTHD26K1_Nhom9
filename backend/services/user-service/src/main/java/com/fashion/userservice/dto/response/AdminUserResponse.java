package com.fashion.userservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminUserResponse {
    private String id;
    private String email;
    private String fullName;
    private String username;
    private String phoneNumber;
    private String role;
    private String status;
    private Boolean enabled;
    private Boolean isBlocked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
