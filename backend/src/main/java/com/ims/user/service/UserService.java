package com.ims.user.service;

import com.ims.global.common.UserType;
import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.global.security.JwtProvider;
import com.ims.user.dto.request.LoginRequest;
import com.ims.user.dto.request.RegisterRequest;
import com.ims.user.dto.response.LoginResponse;
import com.ims.user.dto.response.RegisterResponse;
import com.ims.user.entity.SubUser;
import com.ims.user.entity.User;
import com.ims.user.repository.SubUserRepository;
import com.ims.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final SubUserRepository subUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public RegisterResponse signUp(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ImsException(ErrorCode.DUPLICATE_EMAIL);
        }
        String companyCode = generateUniqueCompanyCode();
        User user = User.register(request.email(), request.password(), request.companyName(),
                companyCode, passwordEncoder);
        userRepository.save(user);
        return new RegisterResponse(companyCode);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ImsException(ErrorCode.USER_NOT_FOUND));

        if (!user.matchesPassword(request.password(), passwordEncoder)) {
            throw new ImsException(ErrorCode.INVALID_PASSWORD);
        }

        String accessToken = jwtProvider.generateAccessToken(user.getId(), UserType.USER, null);
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), UserType.USER);
        return new LoginResponse(accessToken, refreshToken);
    }

    public String refresh(String refreshToken) {
        if (!jwtProvider.isValid(refreshToken) || !jwtProvider.isRefreshToken(refreshToken)) {
            throw new ImsException(ErrorCode.INVALID_TOKEN);
        }
        Long userId = jwtProvider.getUserId(refreshToken);
        UserType userType = jwtProvider.getUserType(refreshToken);

        if (UserType.SUB_USER == userType) {
            SubUser subUser = subUserRepository.findById(userId)
                    .orElseThrow(() -> new ImsException(ErrorCode.SUB_USER_NOT_FOUND));
            return jwtProvider.generateAccessToken(subUser.getId(), UserType.SUB_USER, subUser.getRole());
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ImsException(ErrorCode.USER_NOT_FOUND));
        return jwtProvider.generateAccessToken(user.getId(), UserType.USER, null);
    }

    // companyCode: 대문자+숫자 10자리 랜덤 생성, 충돌 시 재생성
    private String generateUniqueCompanyCode() {
        String code;
        do {
            code = String.format("%010d", ThreadLocalRandom.current().nextLong(0, 10_000_000_000L));
        }
        while (userRepository.existsByCompanyCode(code));

        return code;
    }

}
