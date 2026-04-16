package com.ims.user.dto.response;

public record LoginResponse(
        String accessToken,
        String refreshToken
) {
}
