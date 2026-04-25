package com.ims.item.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BomCreateRequest(
        @NotNull Long childItemId,
        @Min(1) int quantity
) {}
