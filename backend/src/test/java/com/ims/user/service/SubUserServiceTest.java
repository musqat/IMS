package com.ims.user.service;

import com.ims.global.common.Role;
import com.ims.global.exception.ImsException;
import com.ims.global.security.JwtProvider;
import com.ims.user.dto.request.SubUserCreateRequest;
import com.ims.user.dto.request.SubUserLoginRequest;
import com.ims.user.dto.response.LoginResponse;
import com.ims.user.dto.response.SubUserResponse;
import com.ims.user.entity.SubUser;
import com.ims.user.entity.User;
import com.ims.user.repository.SubUserRepository;
import com.ims.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class SubUserServiceTest {

    @InjectMocks
    private SubUserService subUserService;

    @Mock
    private SubUserRepository subUserRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtProvider jwtProvider;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@test.com")
                .password("encodedPassword")
                .companyName("테스트회사")
                .companyCode("1234567890")
                .build();
    }

    @Test
    @DisplayName("생성 성공")
    void createSubUser_success() {
        // given
        SubUserCreateRequest request = new SubUserCreateRequest("sub@test.com", "subPassword", "서브회사", Role.WAREHOUSE);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(subUserRepository.existsByUserIdAndLoginId(1L, "sub@test.com")).willReturn(false);
        given(passwordEncoder.encode(any())).willReturn("encodedPass");
        given(subUserRepository.save(any(SubUser.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        SubUserResponse response = subUserService.createSubUser(1L, request);

        // then
        assertThat(response.loginId()).isEqualTo("sub@test.com");
        then(subUserRepository).should(times(1)).save(any(SubUser.class));
    }

    @Test
    @DisplayName("생성 실패 - 로그인 ID 중복")
    void createSubUser_duplicateLoginId() {
        // given
        SubUserCreateRequest request = new SubUserCreateRequest("sub@test.com", "subPassword", "서브회사", Role.WAREHOUSE);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(subUserRepository.existsByUserIdAndLoginId(1L, "sub@test.com")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> subUserService.createSubUser(1L, request))
                .isInstanceOf(ImsException.class);
    }

    @Test
    @DisplayName("목록 조회 성공")
    void getSubUserList_success() {
        // given
        SubUser subUser = SubUser.builder()
                .user(user)
                .loginId("sub@test.com")
                .password("password")
                .name("서브회사")
                .role(Role.WAREHOUSE)
                .build();
        given(subUserRepository.findAllByUserId(1L)).willReturn(List.of(subUser));

        // when
        List<SubUserResponse> result = subUserService.getSubUserList(1L);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).loginId()).isEqualTo("sub@test.com");
    }

    @Test
    @DisplayName("로그인 성공")
    void subUserLogin_success() {
        // given
        SubUser subUser = SubUser.builder()
                .user(user)
                .loginId("sub@test.com")
                .password("password")
                .name("서브회사")
                .role(Role.WAREHOUSE)
                .build();
        SubUserLoginRequest request = new SubUserLoginRequest("1234567890", "sub@test.com", "password");
        given(subUserRepository.findByUser_CompanyCodeAndLoginId("1234567890", "sub@test.com")).willReturn(Optional.of(subUser));
        given(passwordEncoder.matches("password", "password")).willReturn(true);
        given(jwtProvider.generateAccessToken(any(), any(), any())).willReturn("accessToken");
        given(jwtProvider.generateRefreshToken(any(), any())).willReturn("refreshToken");

        // when
        LoginResponse response = subUserService.subUserLogin(request);

        // then
        assertThat(response.accessToken()).isEqualTo("accessToken");
        assertThat(response.refreshToken()).isEqualTo("refreshToken");
    }

    @Test
    @DisplayName("로그인 실패 - 계정 없음")
    void subUserLogin_notFound() {
        // given
        SubUserLoginRequest request = new SubUserLoginRequest("1234567890", "sub@test.com", "password");
        given(subUserRepository.findByUser_CompanyCodeAndLoginId("1234567890", "sub@test.com")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> subUserService.subUserLogin(request))
                .isInstanceOf(ImsException.class);
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void subUserLogin_invalidPassword() {
        // given
        SubUser subUser = SubUser.builder()
                .user(user)
                .loginId("sub@test.com")
                .password("password")
                .name("서브회사")
                .role(Role.WAREHOUSE)
                .build();
        SubUserLoginRequest request = new SubUserLoginRequest("1234567890", "sub@test.com", "newpassword");
        given(subUserRepository.findByUser_CompanyCodeAndLoginId("1234567890", "sub@test.com")).willReturn(Optional.of(subUser));
        given(passwordEncoder.matches("newpassword", "password")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> subUserService.subUserLogin(request))
                .isInstanceOf(ImsException.class);
    }
}
