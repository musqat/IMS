package com.ims.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.user.dto.LoginRequest;
import com.ims.user.dto.LoginResponse;
import com.ims.user.dto.RegisterRequest;
import com.ims.global.config.SecurityConfig;
import com.ims.global.security.JwtProvider;
import com.ims.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.*;
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
    @DisplayName("회원가입 성공")
    void register_success() throws Exception {
        // given
        RegisterRequest request = new RegisterRequest("test@test.com", "password123", "테스트회사");
        willDoNothing().given(userService).signUp(any());

        // when & then
        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void register_duplicateEmail() throws Exception {
        // given
        RegisterRequest request = new RegisterRequest("test@test.com", "password123", "테스트회사");
        willThrow(new ImsException(ErrorCode.DUPLICATE_EMAIL)).given(userService).signUp(any());

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
        LoginRequest request = new LoginRequest("test@test.com", "password123");
        LoginResponse response = new LoginResponse("accessToken", "refreshToken");
        given(userService.login(any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("accessToken"));
    }
}
