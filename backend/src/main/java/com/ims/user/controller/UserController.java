package com.ims.user.controller;

import com.ims.global.common.ApiResponse;
import com.ims.user.dto.LoginRequest;
import com.ims.user.dto.LoginResponse;
import com.ims.user.dto.RegisterRequest;
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

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@RequestBody @Valid RegisterRequest request) {
        userService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(HttpStatus.CREATED, null));
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
}
