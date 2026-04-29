package com.ims.inventory.dto.request;

import jakarta.validation.constraints.Min;

public record AdjustRequest(
        @Min(0) int newQuantity,
        String memo
) {}
