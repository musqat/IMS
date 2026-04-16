package com.ims.user.dto.request;

import com.ims.global.common.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubUserCreateRequest(
        @NotBlank String loginId,
        @NotBlank String password,
        @NotBlank String name,
        @NotNull Role role   // PRODUCTION 또는 LOGISTICS
) {
}
