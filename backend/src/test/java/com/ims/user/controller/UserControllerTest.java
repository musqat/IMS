package com.ims.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ims.global.common.Role;
import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.global.common.UserType;
import com.ims.global.security.AuthPrincipal;
import com.ims.user.dto.request.LoginRequest;
import com.ims.user.dto.request.RegisterRequest;
import com.ims.user.dto.request.SubUserCreateRequest;
import com.ims.user.dto.request.SubUserLoginRequest;
import com.ims.user.dto.response.LoginResponse;
import com.ims.user.dto.response.RegisterResponse;
import com.ims.user.dto.response.SubUserResponse;
import com.ims.global.config.SecurityConfig;
import com.ims.global.security.JwtProvider;
import com.ims.user.service.SubUserService;
import com.ims.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private SubUserService subUserService;

    @MockitoBean
    private JwtProvider jwtProvider;

    private UsernamePasswordAuthenticationToken mainAuth() {
        return new UsernamePasswordAuthenticationToken(
                new AuthPrincipal(1L, UserType.USER), null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private UsernamePasswordAuthenticationToken productionAuth() {
        return new UsernamePasswordAuthenticationToken(
                new AuthPrincipal(2L, UserType.SUB_USER), null, List.of(new SimpleGrantedAuthority("ROLE_PRODUCTION"))
        );
    }

    // ===== USER =====

    @Test
    @DisplayName("회원가입 성공")
    void register_success() throws Exception {
        // given
        RegisterRequest request = new RegisterRequest("test@test.com", "encodedPassword", "테스트회사");
        given(userService.signUp(any())).willReturn(new RegisterResponse("1234567890"));

        // when & then
        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    @DisplayName("회원가입 실패 - 입력값 오류")
    void register_invalidInput() throws Exception {
        // given
        RegisterRequest request = new RegisterRequest("", "password", "테스트회사");

        // when & then
        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void register_duplicateEmail() throws Exception {
        // given
        RegisterRequest request = new RegisterRequest("test@test.com", "encodedPassword", "테스트회사");
        given(userService.signUp(any())).willThrow(new ImsException(ErrorCode.DUPLICATE_EMAIL));

        // when & then
        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("CONFLICT"));
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() throws Exception {
        // given
        LoginRequest request = new LoginRequest("test@test.com", "encodedPassword");
        given(userService.login(any())).willReturn(new LoginResponse("accessToken", "refreshToken"));

        // when & then
        mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("accessToken"));
    }

    @Test
    @DisplayName("토큰 재발급 성공")
    void refresh_success() throws Exception {
        given(userService.refresh(any())).willReturn("newAccessToken");

        mockMvc.perform(post("/api/v1/users/refresh")
                        .header("Authorization", "Bearer Token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("newAccessToken"));
    }

    // ===== SUB USER =====

    @Test
    @DisplayName("하위 계정 생성 성공")
    void createSubUser_success() throws Exception {
        // given
        SubUserCreateRequest request = new SubUserCreateRequest("sub@test.com", "subPassword", "서브 회사", Role.WAREHOUSE);
        SubUserResponse response = new SubUserResponse(1L, "sub@test.com", "서브 회사", Role.WAREHOUSE, null);
        given(subUserService.createSubUser(eq(1L), any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/users/sub-users")
                        .with(authentication(mainAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.loginId").value("sub@test.com"));
    }

    @Test
    @DisplayName("하위 계정 생성 실패 - 권한 없음")
    void createSubUser_forbidden() throws Exception {
        // given
        SubUserCreateRequest request = new SubUserCreateRequest("sub@test.com", "subPassword", "서브 회사", Role.WAREHOUSE);

        // when & then
        mockMvc.perform(post("/api/v1/users/sub-users")
                        .with(authentication(productionAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("하위 계정 목록 조회 성공")
    void getSubUserList_success() throws Exception {
        // given
        SubUserResponse response = new SubUserResponse(1L, "sub@test.com", "서브 회사", Role.WAREHOUSE, null);
        given(subUserService.getSubUserList(eq(1L))).willReturn(List.of(response));

        // when & then
        mockMvc.perform(get("/api/v1/users/sub-users")
                        .with(authentication(mainAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].loginId").value("sub@test.com"));
    }

    @Test
    @DisplayName("하위 계정 로그인 성공")
    void subLogin_success() throws Exception {
        // given
        SubUserLoginRequest request = new SubUserLoginRequest("1234567890", "test@test.com", "password");
        given(subUserService.subUserLogin(any())).willReturn(new LoginResponse("accessToken", "refreshToken"));

        // when & then
        mockMvc.perform(post("/api/v1/users/sub-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("accessToken"));
    }
}
