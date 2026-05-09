package com.ims.inventory.controller;

import com.ims.global.common.ApiResponse;
import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.inventory.dto.request.InventoryCreateRequest;
import com.ims.inventory.dto.response.InventoryExportRow;
import com.ims.inventory.dto.request.AdjustRequest;
import com.ims.inventory.dto.request.InboundRequest;
import com.ims.inventory.dto.request.OutboundRequest;
import com.ims.inventory.dto.request.SafetyStockUpdateRequest;
import com.ims.inventory.dto.response.InventoryHistoryResponse;
import com.ims.inventory.dto.response.InventoryResponse;
import com.ims.inventory.dto.response.MaxProducibleResponse;
import com.ims.inventory.dto.response.ShortageItemResponse;
import com.ims.inventory.service.InventoryService;
import com.ims.inventory.entity.InventoryHistoryType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/warehouses/{warehouseId}/inventories")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    /** 재고 항목 등록 */
    @PostMapping
    public ResponseEntity<ApiResponse<InventoryResponse>> createInventory(
            @PathVariable Long warehouseId,
            @RequestBody @Valid InventoryCreateRequest request,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(inventoryService.createInventory(userId, warehouseId, request)));
    }

    /** 입고 */
    @PostMapping("/{itemId}/in")
    public ResponseEntity<ApiResponse<InventoryResponse>> adjustIn(
            @PathVariable Long warehouseId,
            @PathVariable Long itemId,
            @RequestBody @Valid InboundRequest request,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.adjustIn(userId, warehouseId, itemId, request)));
    }

    /** 출고 */
    @PostMapping("/{itemId}/out")
    public ResponseEntity<ApiResponse<InventoryResponse>> adjustOut(
            @PathVariable Long warehouseId,
            @PathVariable Long itemId,
            @RequestBody @Valid OutboundRequest request,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.adjustOut(userId, warehouseId, itemId, request)));
    }

    /** 절대값 보정 */
    @PutMapping("/{itemId}/adjust")
    public ResponseEntity<ApiResponse<InventoryResponse>> adjust(
            @PathVariable Long warehouseId,
            @PathVariable Long itemId,
            @RequestBody @Valid AdjustRequest request,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.adjust(userId, warehouseId, itemId, request)));
    }

    /** 재고 목록 조회 (페이지네이션 + 검색) */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<InventoryResponse>>> getInventories(
            @PathVariable Long warehouseId,
            @RequestParam(required = false) String keyword,
            Pageable pageable,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getInventories(userId, warehouseId, keyword, pageable)));
    }

    /** 입출고 이력 조회 */
    @GetMapping("/{itemId}/history")
    public ResponseEntity<ApiResponse<Page<InventoryHistoryResponse>>> getHistory(
            @PathVariable Long warehouseId,
            @PathVariable Long itemId,
            Pageable pageable,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getHistory(userId, warehouseId, itemId, pageable)));
    }

    /** 안전재고 수정 */
    @PatchMapping("/{itemId}/safety-stock")
    public ResponseEntity<ApiResponse<InventoryResponse>> updateSafetyStock(
            @PathVariable Long warehouseId,
            @PathVariable Long itemId,
            @RequestBody @Valid SafetyStockUpdateRequest request,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.updateSafetyStock(userId, warehouseId, itemId, request)));
    }

    /** 창고 전체 이력 Export 조회 (피벗용) */
    @GetMapping("/histories")
    public ResponseEntity<ApiResponse<List<InventoryExportRow>>> getWarehouseHistory(
            @PathVariable Long warehouseId,
            @RequestParam(required = false) List<InventoryHistoryType> types,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal Long userId) {
        if (from.isAfter(to)) {
            throw new ImsException(ErrorCode.INVALID_DATE_RANGE);
        }
        if (from.plusYears(1).isBefore(to)) {
            throw new ImsException(ErrorCode.DATE_RANGE_TOO_LARGE);
        }
        return ResponseEntity.ok(ApiResponse.success(
                inventoryService.getWarehouseHistory(userId, warehouseId, types, from, to)));
    }

    /** 최대 생산 가능 수량 */
    @GetMapping("/{itemId}/max-producible")
    public ResponseEntity<ApiResponse<MaxProducibleResponse>> calcMaxProducible(
            @PathVariable Long warehouseId,
            @PathVariable Long itemId,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.calcMaxProducible(userId, warehouseId, itemId)));
    }

    /** 생산 불가 완제품 분석 */
    @GetMapping("/shortage-analysis")
    public ResponseEntity<ApiResponse<List<ShortageItemResponse>>> getShortageAnalysis(
            @PathVariable Long warehouseId,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getShortageAnalysis(userId, warehouseId)));
    }
}
