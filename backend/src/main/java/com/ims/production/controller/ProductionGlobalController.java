package com.ims.production.controller;

import com.ims.global.common.ApiResponse;
import com.ims.production.dto.response.ProductionCountsResponse;
import com.ims.production.dto.response.ProductionResponse;
import com.ims.production.entity.ProductionStatus;
import com.ims.production.service.ProductionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/productions")
@RequiredArgsConstructor
public class ProductionGlobalController {

    private final ProductionService productionService;

    /** 상태 필터 + 페이지네이션 */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductionResponse>>> getRecordsByStatus(
            @RequestParam ProductionStatus status,
            Pageable pageable,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(
                productionService.getRecordsByStatus(userId, status, pageable)));
    }

    /** 상태별 + ANOMALY 건수 집계 — 대시보드 KPI / 탭 뱃지용 */
    @GetMapping("/counts")
    public ResponseEntity<ApiResponse<ProductionCountsResponse>> getCounts(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(productionService.getStatusCounts(userId)));
    }
}
