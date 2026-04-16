package com.ims.user.dto.response;

import com.ims.global.common.Role;
import com.ims.user.entity.SubUser;

import java.time.LocalDateTime;

public record SubUserResponse(
        Long id,
        String loginId,
        String name,
        Role role,
        LocalDateTime createdAt
) {
    public static SubUserResponse from(SubUser subUser) {
        return new SubUserResponse(
                subUser.getId(),
                subUser.getLoginId(),
                subUser.getName(),
                subUser.getRole(),
                subUser.getCreatedAt()
        );
    }
}
