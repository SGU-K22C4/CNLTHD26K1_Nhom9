package com.fashion.userservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank @Email
    private String email;

    @NotBlank 
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=_!]).{8,100}$", message = "Password must be at least 8 characters, contain one digit, one uppercase, one lowercase and one special character.")
    private String password;

    @NotBlank @Size(max = 100)
    private String fullName;

    @NotBlank
    @Pattern(regexp = "^(84|0[3|5|7|8|9])+([0-9]{8})$", message = "Invalid Vietnamese phone number")
    private String phone;

    private Integer gender; // 0 for male, 1 for female

    private String avatar;

    // Address Info
    private String street;
    private String city;
    private String ward;
    private Boolean isDefault;
}
