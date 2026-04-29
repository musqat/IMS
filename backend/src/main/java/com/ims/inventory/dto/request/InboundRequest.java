package com.ims.inventory.dto.request;

import jakarta.validation.constraints.Min;

public record InboundRequest(
        @Min(1) int quantity,
        String memo
) {}
