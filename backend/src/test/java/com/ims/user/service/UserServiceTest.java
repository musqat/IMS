package com.ims.user.service;

import com.ims.global.exception.ImsException;
import com.ims.user.dto.LoginRequest;
import com.ims.user.dto.LoginResponse;
import com.ims.user.dto.RegisterRequest;
import com.ims.user.entity.User;
import com.ims.user.repository.UserRepository;
import com.ims.global.security.JwtProvider;
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
        given(passwordEncoder.encode(request.password())).willReturn("encodedPassword");

        // when
        userService.signUp(request);

        // then
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
        //given
        User user = User.builder()
                .email("test@test.com")
                .password("encodedPassword")
                .companyName("테스트회사")
                .build();
        LoginRequest request = new LoginRequest("test@test.com", "encodedPassword");

        given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(true);
        given(jwtProvider.generateAccessToken(any(), any())).willReturn("accessToken");
        given(jwtProvider.generateRefreshToken(any())).willReturn("refreshToken");

        //when
        LoginResponse response = userService.login(request);

        //then
        assertThat(response.accessToken()).isEqualTo("accessToken");
        assertThat(response.refreshToken()).isEqualTo("refreshToken");
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
        assertThatThrownBy(() -> userService.login(request)).isInstanceOf(ImsException.class);
    }
}
