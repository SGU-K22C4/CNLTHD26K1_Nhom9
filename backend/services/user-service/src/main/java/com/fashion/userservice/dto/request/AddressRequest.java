package com.fashion.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;    // Thiếu dòng này
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor // Cần cho Jackson/Spring
@AllArgsConstructor // Thêm cái này
public class AddressRequest {

    @NotBlank
    private String fullName;

    @NotBlank
    private String phoneNumber;

    @NotBlank
    private String street;

    @NotBlank
    private String ward;

    @NotBlank
    private String city;

    private boolean isDefault = false;
}
