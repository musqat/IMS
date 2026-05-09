package com.ims.production.dto.request;

import jakarta.validation.constraints.Min;

public record ProductionUpdateRequest(
        @Min(1) int quantity
) {}
