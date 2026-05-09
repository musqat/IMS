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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;

    @Test
    @DisplayName("회원가입 성공 — 토큰 즉시 발급 (auto-login)")
    void signUp_success() {
        RegisterRequest request = new RegisterRequest("test@test.com", "password123", "테스트회사");
        User saved = User.builder().id(1L).email("test@test.com").password("encodedPassword")
                .companyName("테스트회사").companyCode("1000000001").build();
        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(userRepository.existsByCompanyCode(any())).willReturn(false);
        given(passwordEncoder.encode(request.password())).willReturn("encodedPassword");
        given(userRepository.save(any())).willReturn(saved);
        given(jwtProvider.generateAccessToken(1L)).willReturn("accessToken");
        given(jwtProvider.generateRefreshToken(1L)).willReturn("refreshToken");
        given(redisTemplate.opsForValue()).willReturn(valueOps);

        LoginResponse response = userService.signUp(request);

        assertThat(response.accessToken()).isEqualTo("accessToken");
        assertThat(response.refreshToken()).isEqualTo("refreshToken");
        then(userRepository).should(times(1)).save(any(User.class));
        then(valueOps).should().set(eq("refresh:1"), eq("refreshToken"), any());
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signUp_duplicateEmail() {
        RegisterRequest request = new RegisterRequest("test@test.com", "password123", "테스트회사");
        given(userRepository.existsByEmail(request.email())).willReturn(true);

        assertThatThrownBy(() -> userService.signUp(request))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_EMAIL);
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() {
        User user = User.builder().id(1L).email("test@test.com").password("encodedPassword")
                .companyName("테스트회사").build();
        LoginRequest request = new LoginRequest("test@test.com", "encodedPassword");
        given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(true);
        given(jwtProvider.generateAccessToken(any())).willReturn("accessToken");
        given(jwtProvider.generateRefreshToken(any())).willReturn("refreshToken");
        given(redisTemplate.opsForValue()).willReturn(valueOps);

        LoginResponse response = userService.login(request);

        assertThat(response.accessToken()).isEqualTo("accessToken");
        assertThat(response.refreshToken()).isEqualTo("refreshToken");
        then(valueOps).should().set(eq("refresh:1"), eq("refreshToken"), any());
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_invalidPassword() {
        User user = User.builder().email("test@test.com").password("encodedPassword")
                .companyName("테스트회사").build();
        LoginRequest request = new LoginRequest("test@test.com", "wrongPassword");
        given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(false);

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOGIN_FAILED);
    }

    @Test
    @DisplayName("토큰 재발급 성공 - Access/Refresh 모두 재발급 및 Redis 갱신")
    void refresh_success() {
        User user = User.builder().id(1L).email("test@test.com").password("encodedPassword")
                .companyName("테스트회사").build();
        given(jwtProvider.isValid("refreshToken")).willReturn(true);
        given(jwtProvider.isRefreshToken("refreshToken")).willReturn(true);
        given(jwtProvider.getUserId("refreshToken")).willReturn(1L);
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("refresh:1")).willReturn("refreshToken");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(jwtProvider.generateAccessToken(1L)).willReturn("newAccessToken");
        given(jwtProvider.generateRefreshToken(1L)).willReturn("newRefreshToken");

        LoginResponse result = userService.refresh("refreshToken");

        assertThat(result.accessToken()).isEqualTo("newAccessToken");
        assertThat(result.refreshToken()).isEqualTo("newRefreshToken");
        then(valueOps).should().set(eq("refresh:1"), eq("newRefreshToken"), any());
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 유효하지 않은 토큰")
    void refresh_invalidToken() {
        given(jwtProvider.isValid("refreshToken")).willReturn(false);

        assertThatThrownBy(() -> userService.refresh("refreshToken"))
                .isInstanceOf(ImsException.class);
    }

    @Test
    @DisplayName("토큰 재발급 실패 - Redis 토큰 불일치")
    void refresh_redisTokenMismatch() {
        given(jwtProvider.isValid("refreshToken")).willReturn(true);
        given(jwtProvider.isRefreshToken("refreshToken")).willReturn(true);
        given(jwtProvider.getUserId("refreshToken")).willReturn(1L);
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("refresh:1")).willReturn("differentToken");

        assertThatThrownBy(() -> userService.refresh("refreshToken"))
                .isInstanceOf(ImsException.class);
    }

    @Test
    @DisplayName("로그아웃 성공 - 정상 토큰")
    void logout_success() {
        given(jwtProvider.extractUserIdLeniently("refreshToken")).willReturn(1L);

        userService.logout("refreshToken");

        then(redisTemplate).should().delete("refresh:1");
    }

    @Test
    @DisplayName("로그아웃 성공 - 만료된 토큰도 허용 (Redis만 정리)")
    void logout_expiredToken() {
        given(jwtProvider.extractUserIdLeniently("expiredToken")).willReturn(1L);

        userService.logout("expiredToken"); // 예외 없이 정상 처리

        then(redisTemplate).should().delete("refresh:1");
    }

    @Test
    @DisplayName("로그아웃 무시 - 파싱 불가 토큰 (Redis 삭제 없음)")
    void logout_unparsableToken() {
        given(jwtProvider.extractUserIdLeniently("garbage")).willReturn(null);

        userService.logout("garbage"); // 예외 없이 무시

        then(redisTemplate).should(never()).delete(any(String.class));
    }

    @Test
    @DisplayName("내 프로필 조회 성공")
    void getMe_success() {
        User user = User.builder().id(1L).email("test@test.com").password("encoded")
                .companyName("테스트회사").companyCode("1000000001").build();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserResponse response = userService.getMe(1L);

        assertThat(response.email()).isEqualTo("test@test.com");
        assertThat(response.companyName()).isEqualTo("테스트회사");
        assertThat(response.companyCode()).isEqualTo("1000000001");
    }

    @Test
    @DisplayName("회사명 수정 성공")
    void updateCompanyName_success() {
        User user = User.builder().id(1L).email("test@test.com").password("encoded")
                .companyName("구회사명").companyCode("1000000001").build();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserResponse response = userService.updateCompanyName(1L, "신회사명");

        assertThat(response.companyName()).isEqualTo("신회사명");
    }

    @Test
    @DisplayName("비밀번호 변경 성공")
    void updatePassword_success() {
        User user = User.builder().id(1L).email("test@test.com").password("encodedOld")
                .companyName("테스트회사").companyCode("1000000001").build();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("oldPw", "encodedOld")).willReturn(true);
        given(passwordEncoder.matches("newPw", "encodedOld")).willReturn(false);
        given(passwordEncoder.encode("newPw")).willReturn("encodedNew");

        assertThatNoException().isThrownBy(() -> userService.updatePassword(1L, "oldPw", "newPw"));
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 현재 비밀번호 불일치")
    void updatePassword_wrongCurrentPassword() {
        User user = User.builder().id(1L).email("test@test.com").password("encodedOld")
                .companyName("테스트회사").companyCode("1000000001").build();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrongPw", "encodedOld")).willReturn(false);

        assertThatThrownBy(() -> userService.updatePassword(1L, "wrongPw", "newPw"))
                .isInstanceOf(ImsException.class);
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 현재 비밀번호와 동일")
    void updatePassword_sameAsCurrentPassword() {
        User user = User.builder().id(1L).email("test@test.com").password("encodedSame")
                .companyName("테스트회사").companyCode("1000000001").build();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        // currentPassword 검증 통과, 새 비밀번호도 동일 → SAME_AS_CURRENT_PASSWORD
        given(passwordEncoder.matches("samePw", "encodedSame")).willReturn(true);

        assertThatThrownBy(() -> userService.updatePassword(1L, "samePw", "samePw"))
                .isInstanceOf(ImsException.class);
    }

    @Test
    @DisplayName("토큰 재발급 실패 - Redis에 저장된 토큰 없음 (만료 또는 로그아웃)")
    void refresh_redisNull() {
        given(jwtProvider.isValid("refreshToken")).willReturn(true);
        given(jwtProvider.isRefreshToken("refreshToken")).willReturn(true);
        given(jwtProvider.getUserId("refreshToken")).willReturn(1L);
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("refresh:1")).willReturn(null); // Redis에 없음

        assertThatThrownBy(() -> userService.refresh("refreshToken"))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 토큰 유효하지만 DB에 User 없음")
    void refresh_userNotFound() {
        given(jwtProvider.isValid("refreshToken")).willReturn(true);
        given(jwtProvider.isRefreshToken("refreshToken")).willReturn(true);
        given(jwtProvider.getUserId("refreshToken")).willReturn(1L);
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("refresh:1")).willReturn("refreshToken"); // Redis 일치
        given(userRepository.findById(1L)).willReturn(Optional.empty()); // User 없음

        assertThatThrownBy(() -> userService.refresh("refreshToken"))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("로그인 실패 - 이메일 없음")
    void login_emailNotFound() {
        LoginRequest request = new LoginRequest("notexist@test.com", "password");
        given(userRepository.findByEmail("notexist@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOGIN_FAILED);
    }
}
