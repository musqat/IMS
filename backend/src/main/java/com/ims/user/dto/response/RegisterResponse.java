package com.ims.user.dto.response;

import com.ims.user.entity.User;

public record RegisterResponse(
        Long id,
        String email,
        String companyName,
        String companyCode
) {
    public static RegisterResponse from(User user) {
        return new RegisterResponse(
                user.getId(),
                user.getEmail(),
                user.getCompanyName(),
                user.getCompanyCode()
        );
    }
}
