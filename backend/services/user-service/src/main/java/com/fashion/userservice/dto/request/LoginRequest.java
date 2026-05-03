package com.fashion.userservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;    // Thiếu dòng này
import lombok.AllArgsConstructor;
import lombok.Builder;
@Data
@NoArgsConstructor // Cần cho Jackson/Spring
@AllArgsConstructor // Thêm cái này để fix lỗi Test
public class LoginRequest {

    @NotBlank @Email
    private String email;

    @NotBlank
    private String password;
}
