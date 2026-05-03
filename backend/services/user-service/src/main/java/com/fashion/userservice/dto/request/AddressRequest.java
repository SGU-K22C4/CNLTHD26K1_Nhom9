package com.fashion.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
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
