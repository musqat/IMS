package com.ims.user.controller;

import com.ims.global.common.ApiResponse;
import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.user.dto.request.CompanyNameUpdateRequest;
import com.ims.user.dto.request.LoginRequest;
import com.ims.user.dto.request.PasswordUpdateRequest;
import com.ims.user.dto.request.RegisterRequest;
import com.ims.user.dto.response.LoginResponse;
import com.ims.user.dto.response.UserResponse;
import com.ims.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    /** 회원가입 (auto-login) */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<LoginResponse>> register(@RequestBody @Valid RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(userService.signUp(request)));
    }

    /** 로그인 */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest request) {
        LoginResponse response = userService.login(request);

        return ResponseEntity.ok(ApiResponse.success(response));
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

    /** 내 프로필 조회 */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(userService.getMe(userId)));
    }

    /** 회사명 수정 */
    @PatchMapping("/me/company-name")
    public ResponseEntity<ApiResponse<UserResponse>> updateCompanyName(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid CompanyNameUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateCompanyName(userId, request.companyName())));
    }

    /** 비밀번호 변경 */
    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid PasswordUpdateRequest request) {
        userService.updatePassword(userId, request.currentPassword(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /** Access/Refresh 토큰 재발급 (Refresh Token Rotation) */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(@RequestHeader("Authorization") String authHeader) {
        if (!authHeader.startsWith("Bearer ")) {
            throw new ImsException(ErrorCode.INVALID_TOKEN);
        }
        String token = authHeader.substring(7);
        return ResponseEntity.ok(ApiResponse.success(userService.refresh(token)));
    }

}
