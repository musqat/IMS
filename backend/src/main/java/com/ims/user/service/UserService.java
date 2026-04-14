package com.ims.user.service;

import com.ims.global.common.Role;
import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.global.security.JwtProvider;
import com.ims.user.dto.LoginRequest;
import com.ims.user.dto.LoginResponse;
import com.ims.user.dto.RegisterRequest;
import com.ims.user.entity.User;
import com.ims.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public void signUp(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ImsException(ErrorCode.DUPLICATE_EMAIL);
        }
        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .companyName(request.companyName())
                .build();
        userRepository.save(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ImsException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ImsException(ErrorCode.INVALID_PASSWORD);
        }

        String accessToken = jwtProvider.generateAccessToken(user.getId(), Role.MAIN);
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());
        return new LoginResponse(accessToken, refreshToken);
    }

    public String refresh(String refreshToken) {
        if (!jwtProvider.isValid(refreshToken)) {
            throw new ImsException(ErrorCode.INVALID_TOKEN);
        }
        Long userId = jwtProvider.getUserId(refreshToken);
        return jwtProvider.generateAccessToken(userId, Role.MAIN);
    }
}
