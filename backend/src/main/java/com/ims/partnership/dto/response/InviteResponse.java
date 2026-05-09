package com.ims.partnership.dto.response;

public record InviteResponse(
        Long partnershipId,
        String inviteToken
) {}
