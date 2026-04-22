package com.ims.user.service;

import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.global.security.JwtProvider;
import com.ims.user.dto.request.LoginRequest;
import com.ims.user.dto.request.RegisterRequest;
import com.ims.user.dto.response.LoginResponse;
import com.ims.user.entity.User;
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
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    /**
     * 회원가입
     * - 이메일 중복 검증
     * - companyCode 랜덤 생성 (충돌 시 재생성)
     * - 가입 완료 후 바로 토큰 발급 (별도 로그인 불필요)
     */
    @Transactional
    public LoginResponse signUp(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ImsException(ErrorCode.DUPLICATE_EMAIL);
        }
        String companyCode = generateUniqueCompanyCode();
        User user = User.register(request.email(), request.password(), request.companyName(),
                companyCode, passwordEncoder);
        User saved = userRepository.save(user);
        String accessToken = jwtProvider.generateAccessToken(saved.getId());
        String refreshToken = jwtProvider.generateRefreshToken(saved.getId());
        return new LoginResponse(accessToken, refreshToken);
    }

    /**
     * 로그인
     * - 이메일로 User 조회
     * - 비밀번호 검증
     * - Access/Refresh 토큰 발급
     */
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ImsException(ErrorCode.USER_NOT_FOUND));

        if (!user.matchesPassword(request.password(), passwordEncoder)) {
            throw new ImsException(ErrorCode.INVALID_PASSWORD);
        }

        String accessToken = jwtProvider.generateAccessToken(user.getId());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());
        return new LoginResponse(accessToken, refreshToken);
    }

    /**
     * Access 토큰 재발급
     * - Refresh 토큰 유효성 검증
     * - User 존재 여부 확인 후 새 Access 토큰 발급
     */
    public String refresh(String refreshToken) {
        if (!jwtProvider.isValid(refreshToken) || !jwtProvider.isRefreshToken(refreshToken)) {
            throw new ImsException(ErrorCode.INVALID_TOKEN);
        }
        Long userId = jwtProvider.getUserId(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ImsException(ErrorCode.USER_NOT_FOUND));
        return jwtProvider.generateAccessToken(user.getId());
    }

    // 숫자 10자리, DB 충돌 시 재생성
    private String generateUniqueCompanyCode() {
        String code;
        do {
            code = String.format("%010d", ThreadLocalRandom.current().nextLong(0, 10_000_000_000L));
        } while (userRepository.existsByCompanyCode(code));
        return code;
    }
}
