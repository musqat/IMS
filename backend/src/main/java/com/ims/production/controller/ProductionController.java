package com.ims.production.controller;

import com.ims.global.common.ApiResponse;
import com.ims.production.dto.request.ProductionCreateRequest;
import com.ims.production.dto.response.ProductionResponse;
import com.ims.production.service.ProductionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/warehouses/{warehouseId}/productions")
@RequiredArgsConstructor
public class ProductionController {

    private final ProductionService productionService;

    /** 생산 기록 등록 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductionResponse> createRecord(
            @PathVariable Long warehouseId,
            @RequestBody @Valid ProductionCreateRequest request,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(productionService.createRecord(userId, warehouseId, request));
    }

    /** 생산 기록 취소 */
    @DeleteMapping("/{recordId}")
    public ApiResponse<Void> cancelRecord(
            @PathVariable Long recordId,
            @AuthenticationPrincipal Long userId) {
        productionService.cancelRecord(userId, recordId);
        return ApiResponse.success(null);
    }

    /** 생산 기록 목록 조회 */
    @GetMapping
    public ApiResponse<Page<ProductionResponse>> getRecords(
            @PathVariable Long warehouseId,
            Pageable pageable,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(productionService.getRecords(userId, warehouseId, pageable));
    }
}
