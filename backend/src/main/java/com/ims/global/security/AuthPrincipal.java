package com.ims.global.security;

import com.ims.global.common.UserType;

public record AuthPrincipal(Long id, UserType userType) {

    public boolean isUser() {
        return UserType.USER == userType;
    }

    public boolean isSubUser() {
        return UserType.SUB_USER == userType;
    }
}
