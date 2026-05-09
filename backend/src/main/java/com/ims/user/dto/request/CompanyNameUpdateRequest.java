package com.ims.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CompanyNameUpdateRequest(
        @NotBlank String companyName
) {
}
