package com.ims.user.service;

import com.ims.global.common.UserType;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
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

    @Test
    @DisplayName("회원가입 성공")
    void signUp_success() {
        // given
        RegisterRequest request = new RegisterRequest("test@test.com", "password123", "테스트회사");
        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(userRepository.existsByCompanyCode(any())).willReturn(false);
        given(passwordEncoder.encode(request.password())).willReturn("encodedPassword");

        // when
        RegisterResponse response = userService.signUp(request);

        // then
        assertThat(response.companyCode()).isNotNull();
        then(userRepository).should(times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signUp_duplicateEmail() {
        // given
        RegisterRequest request = new RegisterRequest("test@test.com", "password123", "테스트회사");
        given(userRepository.existsByEmail(request.email())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.signUp(request))
                .isInstanceOf(ImsException.class);
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() {
        // given
        User user = User.builder()
                .email("test@test.com")
                .password("encodedPassword")
                .companyName("테스트회사")
                .build();
        LoginRequest request = new LoginRequest("test@test.com", "encodedPassword");
        given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(true);
        given(jwtProvider.generateAccessToken(any(), any(), any())).willReturn("accessToken");
        given(jwtProvider.generateRefreshToken(any(), any())).willReturn("refreshToken");

        // when
        LoginResponse response = userService.login(request);

        // then
        assertThat(response.accessToken()).isEqualTo("accessToken");
        assertThat(response.refreshToken()).isEqualTo("refreshToken");
    }

    @Test
    @DisplayName("토큰 재발급 성공 - USER")
    void refresh_success() {
        // given
        User user = User.builder()
                .id(1L)
                .email("test@test.com")
                .password("encodedPassword")
                .companyName("테스트회사")
                .build();
        given(jwtProvider.isValid("refreshToken")).willReturn(true);
        given(jwtProvider.isRefreshToken("refreshToken")).willReturn(true);
        given(jwtProvider.getUserId("refreshToken")).willReturn(1L);
        given(jwtProvider.getUserType("refreshToken")).willReturn(UserType.USER);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(jwtProvider.generateAccessToken(1L, UserType.USER, null)).willReturn("newAccessToken");

        // when
        String result = userService.refresh("refreshToken");

        // then
        assertThat(result).isEqualTo("newAccessToken");
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 유효하지 않은 토큰")
    void refresh_invalidToken() {
        given(jwtProvider.isValid("refreshToken")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> userService.refresh("refreshToken")).isInstanceOf(ImsException.class);
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_invalidPassword() {
        // given
        User user = User.builder()
                .email("test@test.com")
                .password("encodedPassword")
                .companyName("테스트회사")
                .build();
        LoginRequest request = new LoginRequest("test@test.com", "password");
        given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(false);

        // when & then
        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(ImsException.class);
    }
}
