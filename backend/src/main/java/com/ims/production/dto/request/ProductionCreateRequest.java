package com.ims.production.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProductionCreateRequest(
        @NotNull Long itemId,
        @Min(1) int quantity
) {}
