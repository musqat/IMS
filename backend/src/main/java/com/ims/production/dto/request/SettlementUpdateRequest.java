package com.ims.production.dto.request;

import com.ims.production.entity.SettlementResult;
import jakarta.validation.constraints.NotNull;

public record SettlementUpdateRequest(
        @NotNull SettlementResult result,
        String memo
) {}
