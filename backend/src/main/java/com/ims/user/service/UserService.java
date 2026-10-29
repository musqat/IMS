package com.ims.user.service;

import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.global.security.JwtProvider;
import com.ims.user.dto.request.LoginRequest;
import com.ims.user.dto.request.RegisterRequest;
import com.ims.user.dto.response.LoginResponse;
import com.ims.user.dto.response.UserResponse;
import com.ims.user.entity.User;
import com.ims.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
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
    private final LoginAttemptService loginAttemptService;

    /**
     * 회원가입 (auto-login)
     * - 이메일 중복 검증
     * - companyCode 랜덤 생성 (충돌 시 재생성)
     * - 가입 완료 후 Access/Refresh 토큰 즉시 발급
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
        storeRefreshTokenBestEffort(saved.getId(), refreshToken);
        return new LoginResponse(accessToken, refreshToken);
    }

    /**
     * 로그인
     * - 이메일로 User 조회
     * - 비밀번호 검증
     * - Access/Refresh 토큰 발급 후 Redis에 Refresh 토큰 저장
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        loginAttemptService.checkNotLocked(request.email());

        User user = userRepository.findByEmail(request.email()).orElse(null);
        if (user == null || !user.matchesPassword(request.password(), passwordEncoder)) {
            // 계정이 없을 때도 센다. 세지 않으면 존재하는 계정만 잠겨 열거에 쓰인다
            loginAttemptService.recordFailure(request.email());
            throw new ImsException(ErrorCode.LOGIN_FAILED);
        }
        loginAttemptService.reset(request.email());

        String accessToken = jwtProvider.generateAccessToken(user.getId());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());
        storeRefreshTokenBestEffort(user.getId(), refreshToken);
        return new LoginResponse(accessToken, refreshToken);
    }

    /**
     * Access/Refresh 토큰 재발급 (Refresh Token Rotation)
     * - Refresh 토큰 서명 유효성 검증
     * - Redis에 저장된 토큰과 일치 여부 확인
     * - 새 Access + Refresh 토큰 모두 발급, Redis 갱신 (기존 토큰 즉시 무효화)
     */
    @Transactional
    public LoginResponse refresh(String refreshToken) {
        if (!jwtProvider.isValid(refreshToken) || !jwtProvider.isRefreshToken(refreshToken)) {
            throw new ImsException(ErrorCode.INVALID_TOKEN);
        }
        Long userId = jwtProvider.getUserId(refreshToken);

        // 저장된 토큰과 대조하지 못하면 재발급을 막는다. 회전 검사를 건너뛸 수 없다
        String stored;
        try {
            stored = redisTemplate.opsForValue().get(REFRESH_KEY_PREFIX + userId);
        } catch (DataAccessException e) {
            throw new ImsException(ErrorCode.DEPENDENCY_UNAVAILABLE);
        }
        if (stored == null || !stored.equals(refreshToken)) {
            throw new ImsException(ErrorCode.INVALID_TOKEN);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ImsException(ErrorCode.USER_NOT_FOUND));

        String newAccessToken = jwtProvider.generateAccessToken(user.getId());
        String newRefreshToken = jwtProvider.generateRefreshToken(user.getId());
        try {
            redisTemplate.opsForValue().set(REFRESH_KEY_PREFIX + userId, newRefreshToken, REFRESH_TTL);
        } catch (DataAccessException e) {
            throw new ImsException(ErrorCode.DEPENDENCY_UNAVAILABLE);
        }
        return new LoginResponse(newAccessToken, newRefreshToken);
    }

    /**
     * 로그아웃
     * - 만료된 토큰도 허용 (클라이언트가 만료 후 로그아웃 요청하는 경우 정상 처리)
     * - userId 추출 가능 시 Redis에서 삭제, 파싱 불가 토큰은 무시(이미 무효)
     */
    @Transactional
    public void logout(String refreshToken) {
        Long userId = jwtProvider.extractUserIdLeniently(refreshToken);
        if (userId == null) return; // 완전히 유효하지 않은 토큰 — Redis 삭제 불필요
        try {
            redisTemplate.delete(REFRESH_KEY_PREFIX + userId);
        } catch (DataAccessException e) {
            // 지우지 못해도 로그아웃은 끝낸다. 남은 토큰은 TTL이 지나면 사라진다
            log.warn("Refresh 토큰 삭제 실패 userId={}", userId, e);
        }
    }

    /**
     * 내 프로필 조회
     */
    public UserResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ImsException(ErrorCode.USER_NOT_FOUND));
        return UserResponse.from(user);
    }

    /**
     * 회사명 수정
     */
    @Transactional
    public UserResponse updateCompanyName(Long userId, String companyName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ImsException(ErrorCode.USER_NOT_FOUND));
        user.updateCompanyName(companyName);
        return UserResponse.from(user);
    }

    /**
     * 비밀번호 변경
     * - 현재 비밀번호 BCrypt 검증
     * - 새 비밀번호 BCrypt 인코딩 후 저장
     */
    @Transactional
    public void updatePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ImsException(ErrorCode.USER_NOT_FOUND));
        if (!user.matchesPassword(currentPassword, passwordEncoder)) {
            throw new ImsException(ErrorCode.INVALID_PASSWORD);
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new ImsException(ErrorCode.SAME_AS_CURRENT_PASSWORD);
        }
        user.updatePassword(passwordEncoder.encode(newPassword));
    }

    /**
     * Refresh 토큰 저장. 실패해도 로그인은 성공시킨다
     *
     * Access 토큰은 이미 발급돼 15분간 쓸 수 있다. 저장만 못 했으니
     * 만료 뒤 재발급이 막히고 다시 로그인하면 된다.
     */
    private void storeRefreshTokenBestEffort(Long userId, String refreshToken) {
        try {
            redisTemplate.opsForValue().set(REFRESH_KEY_PREFIX + userId, refreshToken, REFRESH_TTL);
        } catch (DataAccessException e) {
            log.warn("Refresh 토큰 저장 실패 userId={}", userId, e);
        }
    }

    /** 랜덤 숫자(10자리) 생성, 충돌 시 재생성 (최대 10회 재시도) */
    private String generateUniqueCompanyCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String code = String.format("%010d", ThreadLocalRandom.current().nextLong(0, 10_000_000_000L));
            if (!userRepository.existsByCompanyCode(code)) {
                return code;
            }
        }
        throw new ImsException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
