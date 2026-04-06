package com.fashion.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @NotBlank @Size(max = 100)
    private String fullName;

    @Size(max = 20)
    private String phone;
}
