package com.fashion.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;    // Thiếu dòng này
import lombok.AllArgsConstructor;
import lombok.Builder;
@Data
@NoArgsConstructor // Cần cho Jackson/Spring
@AllArgsConstructor // Thêm cái này
public class ChangePasswordRequest {

    @NotBlank
    private String currentPassword;

    @NotBlank
    @Size(min = 8, max = 100)
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=_!]).{8,100}$",
            message = "Password must be at least 8 characters, contain one digit, one uppercase, one lowercase and one special character.")
    private String newPassword;
}
