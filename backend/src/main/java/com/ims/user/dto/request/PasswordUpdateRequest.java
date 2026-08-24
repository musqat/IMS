package com.ims.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordUpdateRequest(
        @NotBlank String currentPassword,
        @NotBlank
        @Size(min = PasswordPolicy.MIN_LENGTH, message = PasswordPolicy.MESSAGE)
        @Pattern(regexp = PasswordPolicy.PATTERN, message = PasswordPolicy.MESSAGE)
        String newPassword
) {
}
