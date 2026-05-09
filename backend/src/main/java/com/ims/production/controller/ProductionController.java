package com.ims.production.controller;

import com.ims.global.common.ApiResponse;
import com.ims.production.dto.request.ProductionCreateRequest;
import com.ims.production.dto.request.ProductionUpdateRequest;
import com.ims.production.dto.request.SettlementUpdateRequest;
import com.ims.production.dto.response.ProductionResponse;
import com.ims.production.service.ProductionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/warehouses/{warehouseId}/productions")
@RequiredArgsConstructor
public class ProductionController {

    private final ProductionService productionService;

    /** 생산 기록 등록 */
    @PostMapping
    public ResponseEntity<ApiResponse<ProductionResponse>> createRecord(
            @PathVariable Long warehouseId,
            @RequestBody @Valid ProductionCreateRequest request,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(productionService.createRecord(userId, warehouseId, request)));
    }

    /** 생산 기록 수정 */
    @PatchMapping("/{recordId}")
    public ResponseEntity<ApiResponse<ProductionResponse>> updateRecord(
            @PathVariable Long warehouseId,
            @PathVariable Long recordId,
            @RequestBody @Valid ProductionUpdateRequest request,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(productionService.updateRecord(userId, warehouseId, recordId, request)));
    }

    /** 결산 수정 */
    @PatchMapping("/{recordId}/settlement")
    public ResponseEntity<ApiResponse<ProductionResponse>> updateSettlement(
            @PathVariable Long warehouseId,
            @PathVariable Long recordId,
            @RequestBody @Valid SettlementUpdateRequest request,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(productionService.updateSettlement(userId, warehouseId, recordId, request)));
    }

    /** 강제 결산 */
    @PostMapping("/{recordId}/settle")
    public ResponseEntity<ApiResponse<ProductionResponse>> forceSettle(
            @PathVariable Long warehouseId,
            @PathVariable Long recordId,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(productionService.forceSettle(userId, warehouseId, recordId)));
    }

    /** 생산 기록 취소 */
    @DeleteMapping("/{recordId}")
    public ResponseEntity<ApiResponse<Void>> cancelRecord(
            @PathVariable Long warehouseId,
            @PathVariable Long recordId,
            @AuthenticationPrincipal Long userId) {
        productionService.cancelRecord(userId, warehouseId, recordId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /** 생산 기록 목록 조회 */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductionResponse>>> getRecords(
            @PathVariable Long warehouseId,
            Pageable pageable,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(productionService.getRecords(userId, warehouseId, pageable)));
    }
}
