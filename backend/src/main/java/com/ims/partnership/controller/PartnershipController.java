package com.ims.partnership.controller;

import com.ims.global.common.ApiResponse;
import com.ims.partnership.dto.request.InviteRequest;
import com.ims.partnership.dto.response.PartnershipResponse;
import com.ims.partnership.service.PartnershipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/partnerships")
public class PartnershipController {

    private final PartnershipService partnershipService;

    // 초대 발송 → 토큰 반환
    @PostMapping("/invite")
    public ResponseEntity<ApiResponse<String>> invite(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid InviteRequest request
    ) {
        String token = partnershipService.invite(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(token));
    }

    // 초대 수락
    @PostMapping("/accept")
    public ResponseEntity<ApiResponse<PartnershipResponse>> accept(
            @AuthenticationPrincipal Long userId,
            @RequestParam String token
    ) {
        PartnershipResponse response = partnershipService.accept(userId, token);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 내가 본사인 경우 → 하청 목록
    @GetMapping("/subs")
    public ResponseEntity<ApiResponse<List<PartnershipResponse>>> getSubList(
            @AuthenticationPrincipal Long userId
    ) {
        List<PartnershipResponse> response = partnershipService.getSubList(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 내가 하청인 경우 → 본사 목록
    @GetMapping("/mains")
    public ResponseEntity<ApiResponse<List<PartnershipResponse>>> getMainList(
            @AuthenticationPrincipal Long userId
    ) {
        List<PartnershipResponse> response = partnershipService.getMainList(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
