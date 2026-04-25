package com.ims.user.controller;

import com.ims.global.common.ApiResponse;
import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.user.dto.request.LoginRequest;
import com.ims.user.dto.request.RegisterRequest;
import com.ims.user.dto.response.LoginResponse;
import com.ims.user.dto.response.RegisterResponse;
import com.ims.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    /** 회원가입 */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@RequestBody @Valid RegisterRequest request) {
        RegisterResponse response = userService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /** 로그인 */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest request) {
        LoginResponse response = userService.login(request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** Access 토큰 재발급 */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> refresh(@RequestHeader("Authorization") String authHeader) {
        if (!authHeader.startsWith("Bearer ")) {
            throw new ImsException(ErrorCode.INVALID_TOKEN);
        }
        String token = authHeader.substring(7);
        String newAccessToken = userService.refresh(token);
        return ResponseEntity.ok(ApiResponse.success(newAccessToken));
    }

    /** 로그아웃 — Redis에서 Refresh Token 삭제 */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String authHeader) {
        if (!authHeader.startsWith("Bearer ")) {
            throw new ImsException(ErrorCode.INVALID_TOKEN);
        }
        userService.logout(authHeader.substring(7));
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
