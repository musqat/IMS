package com.ims.user.service;

import com.ims.global.common.UserType;
import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.user.dto.request.SubUserCreateRequest;
import com.ims.user.dto.request.SubUserLoginRequest;
import com.ims.user.dto.response.LoginResponse;
import com.ims.user.dto.response.SubUserResponse;
import com.ims.global.security.JwtProvider;
import com.ims.user.entity.SubUser;
import com.ims.user.entity.User;
import com.ims.user.repository.SubUserRepository;
import com.ims.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubUserService {

    private final SubUserRepository subUserRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public SubUserResponse createSubUser(Long userId, SubUserCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ImsException(ErrorCode.USER_NOT_FOUND));

        if (subUserRepository.existsByUserIdAndLoginId(userId, request.loginId())) {
            throw new ImsException(ErrorCode.DUPLICATE_LOGIN_ID);
        }

        SubUser subUser = SubUser.builder()
                .user(user)
                .loginId(request.loginId())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .role(request.role())
                .build();

        SubUser saved = subUserRepository.save(subUser);
        return SubUserResponse.from(saved);
    }

    public List<SubUserResponse> getSubUserList(Long userId) {
        return subUserRepository.findAllByUserId(userId).stream()
                .map(SubUserResponse::from)
                .toList();
    }

    public LoginResponse subUserLogin(SubUserLoginRequest request) {
        SubUser subUser = subUserRepository.findByUser_CompanyCodeAndLoginId(request.companyCode(), request.loginId())
                .orElseThrow(() -> new ImsException(ErrorCode.SUB_USER_NOT_FOUND));

        if (!subUser.matchesPassword(request.password(), passwordEncoder)) {
            throw new ImsException(ErrorCode.INVALID_PASSWORD);
        }

        String accessToken = jwtProvider.generateAccessToken(subUser.getId(), UserType.SUB_USER, subUser.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(subUser.getId(), UserType.SUB_USER);

        return new LoginResponse(accessToken, refreshToken);
    }
}
