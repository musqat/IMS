package com.ims.warehouse.dto.request;

import com.ims.warehouse.entity.WarehouseShare.SharePermission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ShareRequest(
        @NotBlank String companyCode,
        @NotNull SharePermission permission
) {}
