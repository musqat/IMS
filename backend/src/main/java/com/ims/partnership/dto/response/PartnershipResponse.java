package com.ims.partnership.dto.response;

import com.ims.partnership.entity.Partnership;

import java.time.LocalDateTime;

public record PartnershipResponse(
        Long id,
        Long mainId,
        String mainCompanyName,
        Long subId,
        String subCompanyName,
        String subCompanyCode,
        String status,
        LocalDateTime acceptedAt,
        String alias
) {
    public static PartnershipResponse from(Partnership partnership) {
        return new PartnershipResponse(
                partnership.getId(),
                partnership.getMain().getId(),
                partnership.getMain().getCompanyName(),
                partnership.getSub().getId(),
                partnership.getSub().getCompanyName(),
                partnership.getSub().getCompanyCode(),
                partnership.getStatus().name(),
                partnership.getAcceptedAt(),
                partnership.getAlias()
        );
    }
}
