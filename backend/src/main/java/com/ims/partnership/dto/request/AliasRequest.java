package com.ims.partnership.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AliasRequest(
        @NotBlank @Size(max = 50) String alias
) {}
