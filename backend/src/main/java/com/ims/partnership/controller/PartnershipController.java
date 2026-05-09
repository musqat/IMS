package com.ims.partnership.controller;

import com.ims.global.common.ApiResponse;
import com.ims.partnership.dto.request.AliasRequest;
import com.ims.partnership.dto.request.InviteRequest;
import com.ims.partnership.dto.response.InviteResponse;
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

    /** 초대 발송 — partnershipId + inviteToken만 반환 (보안: 토큰은 생성 시 1회만 노출) */
    @PostMapping("/invite")
    public ResponseEntity<ApiResponse<InviteResponse>> invite(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid InviteRequest request
    ) {
        InviteResponse response = partnershipService.invite(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /** PENDING 초대 취소 (본사만 가능) */
    @DeleteMapping("/{partnershipId}/invite")
    public ResponseEntity<ApiResponse<Void>> cancelInvite(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long partnershipId
    ) {
        partnershipService.cancelInvite(userId, partnershipId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /** 초대 수락 */
    @PostMapping("/accept")
    public ResponseEntity<ApiResponse<PartnershipResponse>> accept(
            @AuthenticationPrincipal Long userId,
            @RequestParam String token
    ) {
        PartnershipResponse response = partnershipService.accept(userId, token);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** 하청 목록 조회 (본사 기준) */
    @GetMapping("/subs")
    public ResponseEntity<ApiResponse<List<PartnershipResponse>>> getSubList(
            @AuthenticationPrincipal Long userId
    ) {
        List<PartnershipResponse> response = partnershipService.getSubList(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** 본사 목록 조회 (하청 기준) */
    @GetMapping("/mains")
    public ResponseEntity<ApiResponse<List<PartnershipResponse>>> getMainList(
            @AuthenticationPrincipal Long userId
    ) {
        List<PartnershipResponse> response = partnershipService.getMainList(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** 파트너십 해제 */
    @DeleteMapping("/{partnershipId}")
    public ResponseEntity<ApiResponse<Void>> removePartnership(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long partnershipId
    ) {
        partnershipService.removePartnership(userId, partnershipId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /** 별명(alias) 설정 */
    @PatchMapping("/{partnershipId}/alias")
    public ResponseEntity<ApiResponse<PartnershipResponse>> updateAlias(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long partnershipId,
            @RequestBody @Valid AliasRequest request
    ) {
        PartnershipResponse response = partnershipService.updateAlias(userId, partnershipId, request.alias());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
