package com.ims.inventory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InventoryCreateRequest(
        @NotNull Long itemId,
        @Min(0) int quantity,
        @Min(0) int safetyStock
) {}
