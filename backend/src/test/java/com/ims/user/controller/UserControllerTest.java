package com.ims.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.global.config.SecurityConfig;
import com.ims.global.security.JwtProvider;
import com.ims.user.dto.request.CompanyNameUpdateRequest;
import com.ims.user.dto.request.LoginRequest;
import com.ims.user.dto.request.PasswordUpdateRequest;
import com.ims.user.dto.request.RegisterRequest;
import com.ims.user.dto.response.LoginResponse;
import com.ims.user.dto.response.UserResponse;
import com.ims.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

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
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("회원가입 성공 — 201 + 토큰 즉시 반환")
    void register_success() throws Exception {
        RegisterRequest request = new RegisterRequest("test@test.com", "password", "테스트회사");
        given(userService.signUp(any())).willReturn(new LoginResponse("accessToken", "refreshToken"));

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.accessToken").value("accessToken"))
                .andExpect(jsonPath("$.data.refreshToken").value("refreshToken"));
    }

    @Test
    @DisplayName("회원가입 실패 - 입력값 오류")
    void register_invalidInput() throws Exception {
        RegisterRequest request = new RegisterRequest("", "password", "테스트회사");

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void register_duplicateEmail() throws Exception {
        RegisterRequest request = new RegisterRequest("test@test.com", "password", "테스트회사");
        given(userService.signUp(any())).willThrow(new ImsException(ErrorCode.DUPLICATE_EMAIL));

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() throws Exception {
        LoginRequest request = new LoginRequest("test@test.com", "password");
        given(userService.login(any())).willReturn(new LoginResponse("accessToken", "refreshToken"));

        mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("accessToken"));
    }

    @Test
    @DisplayName("토큰 재발급 성공 - Access/Refresh 모두 반환")
    void refresh_success() throws Exception {
        given(userService.refresh(any())).willReturn(new LoginResponse("newAccessToken", "newRefreshToken"));

        mockMvc.perform(post("/api/v1/users/refresh")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("newAccessToken"))
                .andExpect(jsonPath("$.data.refreshToken").value("newRefreshToken"));
    }

    @Test
    @DisplayName("내 프로필 조회 성공 - 200 OK")
    void getMe_success() throws Exception {
        UserResponse response = new UserResponse(1L, "test@test.com", "테스트회사", "1000000001");
        given(userService.getMe(1L)).willReturn(response);

        mockMvc.perform(get("/api/v1/users/me")
                        .with(authentication(auth(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("test@test.com"));
    }

    @Test
    @DisplayName("회사명 수정 성공 - 200 OK")
    void updateCompanyName_success() throws Exception {
        UserResponse response = new UserResponse(1L, "test@test.com", "신회사명", "1000000001");
        given(userService.updateCompanyName(eq(1L), eq("신회사명"))).willReturn(response);

        mockMvc.perform(patch("/api/v1/users/me/company-name")
                        .with(authentication(auth(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CompanyNameUpdateRequest("신회사명"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companyName").value("신회사명"));
    }

    @Test
    @DisplayName("비밀번호 변경 성공 - 200 OK")
    void updatePassword_success() throws Exception {
        willDoNothing().given(userService).updatePassword(eq(1L), any(), any());

        mockMvc.perform(patch("/api/v1/users/me/password")
                        .with(authentication(auth(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PasswordUpdateRequest("oldPw1234", "newPw5678"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 미인증 401")
    void updatePassword_unauthorized() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PasswordUpdateRequest("oldPw1234", "newPw5678"))))
                .andExpect(status().isUnauthorized());
    }

    private UsernamePasswordAuthenticationToken auth(Long id) {
        return new UsernamePasswordAuthenticationToken(
                id, null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
