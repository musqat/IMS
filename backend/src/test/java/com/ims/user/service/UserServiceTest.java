package com.ims.user.service;

import com.ims.global.exception.ImsException;
import com.ims.global.security.JwtProvider;
import com.ims.user.dto.request.LoginRequest;
import com.ims.user.dto.request.RegisterRequest;
import com.ims.user.dto.response.LoginResponse;
import com.ims.user.dto.response.RegisterResponse;
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
    @DisplayName("회원가입 성공")
    void signUp_success() {
        RegisterRequest request = new RegisterRequest("test@test.com", "password123", "테스트회사");
        User saved = User.builder().id(1L).email("test@test.com").password("encodedPassword")
                .companyName("테스트회사").companyCode("1000000001").build();
        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(userRepository.existsByCompanyCode(any())).willReturn(false);
        given(passwordEncoder.encode(request.password())).willReturn("encodedPassword");
        given(userRepository.save(any())).willReturn(saved);

        RegisterResponse response = userService.signUp(request);

        assertThat(response.email()).isEqualTo("test@test.com");
        assertThat(response.companyName()).isEqualTo("테스트회사");
        assertThat(response.companyCode()).isEqualTo("1000000001");
        then(userRepository).should(times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signUp_duplicateEmail() {
        RegisterRequest request = new RegisterRequest("test@test.com", "password123", "테스트회사");
        given(userRepository.existsByEmail(request.email())).willReturn(true);

        assertThatThrownBy(() -> userService.signUp(request))
                .isInstanceOf(ImsException.class);
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
                .isInstanceOf(ImsException.class);
    }

    @Test
    @DisplayName("토큰 재발급 성공")
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

        String result = userService.refresh("refreshToken");

        assertThat(result).isEqualTo("newAccessToken");
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
    @DisplayName("로그아웃 성공")
    void logout_success() {
        given(jwtProvider.isValid("refreshToken")).willReturn(true);
        given(jwtProvider.isRefreshToken("refreshToken")).willReturn(true);
        given(jwtProvider.getUserId("refreshToken")).willReturn(1L);

        userService.logout("refreshToken");

        then(redisTemplate).should().delete("refresh:1");
    }

    @Test
    @DisplayName("로그아웃 실패 - 유효하지 않은 토큰")
    void logout_invalidToken() {
        given(jwtProvider.isValid("refreshToken")).willReturn(false);

        assertThatThrownBy(() -> userService.logout("refreshToken"))
                .isInstanceOf(ImsException.class);
    }
}
