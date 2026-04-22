package com.ims.partnership.dto.request;

import jakarta.validation.constraints.NotBlank;

public record InviteRequest(
        @NotBlank String companyCode  // 초대할 회사의 companyCode
) {}
