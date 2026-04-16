package com.ims.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SubUserLoginRequest(
        @NotBlank String companyCode,
        @NotBlank String loginId,
        @NotBlank String password
) {
}
