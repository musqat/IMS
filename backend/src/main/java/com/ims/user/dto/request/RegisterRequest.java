package com.ims.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email @NotBlank
        String email,
        @NotBlank
        @Size(min = PasswordPolicy.MIN_LENGTH, message = PasswordPolicy.MESSAGE)
        @Pattern(regexp = PasswordPolicy.PATTERN, message = PasswordPolicy.MESSAGE)
        String password,
        @NotBlank
        String companyName
) {
}
