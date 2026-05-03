package com.fashion.userservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;    // Thiếu dòng này
import lombok.AllArgsConstructor;
import lombok.Builder;
@Data
@NoArgsConstructor
@AllArgsConstructor // Thêm cái này
public class ForgotPasswordRequest {

    @NotBlank @Email
    private String email;
}
