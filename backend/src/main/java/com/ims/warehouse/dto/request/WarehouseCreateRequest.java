package com.ims.warehouse.dto.request;

import jakarta.validation.constraints.NotBlank;

public record WarehouseCreateRequest(
        @NotBlank String name,
        String location
) {}
