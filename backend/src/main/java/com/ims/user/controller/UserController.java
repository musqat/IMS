package com.ims.user.controller;

import com.ims.global.common.ApiResponse;
import com.ims.global.security.AuthPrincipal;
import com.ims.user.dto.request.LoginRequest;
import com.ims.user.dto.request.RegisterRequest;
import com.ims.user.dto.request.SubUserCreateRequest;
import com.ims.user.dto.request.SubUserLoginRequest;
import com.ims.user.dto.response.LoginResponse;
import com.ims.user.dto.response.RegisterResponse;
import com.ims.user.dto.response.SubUserResponse;
import com.ims.user.service.SubUserService;
import com.ims.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final SubUserService subUserService;

    // ===== USER =====

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@RequestBody @Valid RegisterRequest request) {
        RegisterResponse response = userService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(HttpStatus.CREATED, response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest request) {
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> refresh(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring("Bearer ".length());
        String newAccessToken = userService.refresh(token);
        return ResponseEntity.ok(ApiResponse.success(newAccessToken));
    }

    // ===== SUB USER =====

    @PostMapping("/sub-users")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<SubUserResponse>> createSubUser(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestBody @Valid SubUserCreateRequest request
    ) {
        SubUserResponse response = subUserService.createSubUser(principal.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(HttpStatus.CREATED, response));
    }

    @GetMapping("/sub-users")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<SubUserResponse>>> getSubUserList(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        List<SubUserResponse> response = subUserService.getSubUserList(principal.id());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/sub-login")
    public ResponseEntity<ApiResponse<LoginResponse>> subLogin(@RequestBody @Valid SubUserLoginRequest request) {
        LoginResponse response = subUserService.subUserLogin(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
