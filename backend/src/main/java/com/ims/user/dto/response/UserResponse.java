package com.ims.user.dto.response;

import com.ims.user.entity.User;

public record UserResponse(
        Long id,
        String email,
        String companyName,
        String companyCode
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getCompanyName(),
                user.getCompanyCode()
        );
    }
}
