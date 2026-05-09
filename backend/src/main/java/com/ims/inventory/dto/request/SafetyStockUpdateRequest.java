package com.ims.inventory.dto.request;

import jakarta.validation.constraints.Min;

public record SafetyStockUpdateRequest(
        @Min(0) int safetyStock
) {}
