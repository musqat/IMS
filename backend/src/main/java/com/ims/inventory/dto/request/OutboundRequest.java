package com.ims.inventory.dto.request;

import jakarta.validation.constraints.Min;

public record OutboundRequest(
        @Min(1) int quantity,
        String memo
) {}
