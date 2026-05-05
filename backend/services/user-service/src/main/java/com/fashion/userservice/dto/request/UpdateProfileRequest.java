package com.fashion.userservice.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(max = 100)
    private String fullName;

    @Size(max = 50)
    private String firstName;

    @Size(max = 50)
    private String lastName;

    @JsonAlias("phoneNumber")
    @Size(max = 20)
    private String phone;

    /**
     * Frontend profile form currently submits firstName/lastName separately,
     * while older clients may still send fullName. Accept both shapes.
     */
    @AssertTrue(message = "Full name is required")
    public boolean hasAnyName() {
        return hasText(fullName) || hasText(firstName) || hasText(lastName);
    }

    public String resolveFullName() {
        if (hasText(fullName)) {
            return fullName.trim();
        }

        String combinedName = String.join(" ",
                normalizeNamePart(lastName),
                normalizeNamePart(firstName)).trim();

        return combinedName.replaceAll("\\s+", " ");
    }

    public String resolvePhone() {
        return hasText(phone) ? phone.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String normalizeNamePart(String value) {
        return hasText(value) ? value.trim() : "";
    }
}
