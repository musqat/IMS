package com.ims.user.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken
) {
}
