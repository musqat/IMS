package com.ims.user.service;

import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.global.security.JwtProvider;
import com.ims.user.dto.request.LoginRequest;
import com.ims.user.dto.request.RegisterRequest;
import com.ims.user.dto.response.LoginResponse;
import com.ims.user.dto.response.RegisterResponse;
import com.ims.user.entity.User;
import com.ims.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private static final String REFRESH_KEY_PREFIX = "refresh:";
    private static final Duration REFRESH_TTL = Duration.ofDays(14);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final StringRedisTemplate redisTemplate;

    /**
     * 회원가입
     * - 이메일 중복 검증
     * - companyCode 랜덤 생성 (충돌 시 재생성)
     * - 가입 완료 후 유저 정보 반환 (토큰 발급 없음 — 별도 로그인 필요)
     */
    @Transactional
    public RegisterResponse signUp(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ImsException(ErrorCode.DUPLICATE_EMAIL);
        }
        String companyCode = generateUniqueCompanyCode();
        User user = User.register(request.email(), request.password(), request.companyName(),
                companyCode, passwordEncoder);
        return RegisterResponse.from(userRepository.save(user));
    }

    /**
     * 로그인
     * - 이메일로 User 조회
     * - 비밀번호 검증
     * - Access/Refresh 토큰 발급 후 Redis에 Refresh 토큰 저장
     */
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ImsException(ErrorCode.USER_NOT_FOUND));

        if (!user.matchesPassword(request.password(), passwordEncoder)) {
            throw new ImsException(ErrorCode.INVALID_PASSWORD);
        }

        String accessToken = jwtProvider.generateAccessToken(user.getId());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());
        redisTemplate.opsForValue().set(REFRESH_KEY_PREFIX + user.getId(), refreshToken, REFRESH_TTL);
        return new LoginResponse(accessToken, refreshToken);
    }

    /**
     * Access 토큰 재발급
     * - Refresh 토큰 서명 유효성 검증
     * - Redis에 저장된 토큰과 일치 여부 확인
     * - 새 Access 토큰 발급
     */
    public String refresh(String refreshToken) {
        if (!jwtProvider.isValid(refreshToken) || !jwtProvider.isRefreshToken(refreshToken)) {
            throw new ImsException(ErrorCode.INVALID_TOKEN);
        }
        Long userId = jwtProvider.getUserId(refreshToken);

        String stored = redisTemplate.opsForValue().get(REFRESH_KEY_PREFIX + userId);
        if (stored == null || !stored.equals(refreshToken)) {
            throw new ImsException(ErrorCode.INVALID_TOKEN);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ImsException(ErrorCode.USER_NOT_FOUND));
        return jwtProvider.generateAccessToken(user.getId());
    }

    /**
     * 로그아웃
     * - Refresh 토큰 유효성 검증
     * - Redis에서 Refresh 토큰 삭제 (즉시 무효화)
     */
    public void logout(String refreshToken) {
        if (!jwtProvider.isValid(refreshToken) || !jwtProvider.isRefreshToken(refreshToken)) {
            throw new ImsException(ErrorCode.INVALID_TOKEN);
        }
        Long userId = jwtProvider.getUserId(refreshToken);
        redisTemplate.delete(REFRESH_KEY_PREFIX + userId);
    }

    // 숫자 10자리, 충돌 시 재생성
    private String generateUniqueCompanyCode() {
        String code;
        do {
            code = String.format("%010d", ThreadLocalRandom.current().nextLong(0, 10_000_000_000L));
        } while (userRepository.existsByCompanyCode(code));
        return code;
    }
}
