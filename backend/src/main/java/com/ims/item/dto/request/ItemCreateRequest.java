package com.ims.item.dto.request;

import com.ims.item.entity.ItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ItemCreateRequest(
        @NotBlank String itemCode,
        @NotBlank String name,
        @NotNull ItemType type,
        String description
) {}
