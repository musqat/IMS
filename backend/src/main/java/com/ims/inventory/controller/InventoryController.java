package com.ims.inventory.controller;

import com.ims.global.common.ApiResponse;
import com.ims.inventory.dto.request.InventoryCreateRequest;
import com.ims.inventory.dto.request.AdjustRequest;
import com.ims.inventory.dto.request.InboundRequest;
import com.ims.inventory.dto.request.OutboundRequest;
import com.ims.inventory.dto.response.InventoryHistoryResponse;
import com.ims.inventory.dto.response.InventoryResponse;
import com.ims.inventory.dto.response.MaxProducibleResponse;
import com.ims.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/warehouses/{warehouseId}/inventories")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    /** 재고 항목 등록 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InventoryResponse> createInventory(
            @PathVariable Long warehouseId,
            @RequestBody @Valid InventoryCreateRequest request,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(inventoryService.createInventory(userId, warehouseId, request));
    }

    /** 입고 */
    @PostMapping("/{itemId}/in")
    public ApiResponse<InventoryResponse> adjustIn(
            @PathVariable Long warehouseId,
            @PathVariable Long itemId,
            @RequestBody @Valid InboundRequest request,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(inventoryService.adjustIn(userId, warehouseId, itemId, request));
    }

    /** 출고 */
    @PostMapping("/{itemId}/out")
    public ApiResponse<InventoryResponse> adjustOut(
            @PathVariable Long warehouseId,
            @PathVariable Long itemId,
            @RequestBody @Valid OutboundRequest request,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(inventoryService.adjustOut(userId, warehouseId, itemId, request));
    }

    /** 절대값 보정 */
    @PutMapping("/{itemId}/adjust")
    public ApiResponse<InventoryResponse> adjust(
            @PathVariable Long warehouseId,
            @PathVariable Long itemId,
            @RequestBody @Valid AdjustRequest request,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(inventoryService.adjust(userId, warehouseId, itemId, request));
    }

    /** 재고 목록 조회 (페이지네이션 + 검색) */
    @GetMapping
    public ApiResponse<Page<InventoryResponse>> getInventories(
            @PathVariable Long warehouseId,
            @RequestParam(required = false) String keyword,
            Pageable pageable,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(inventoryService.getInventories(userId, warehouseId, keyword, pageable));
    }

    /** 입출고 이력 조회 */
    @GetMapping("/{itemId}/history")
    public ApiResponse<Page<InventoryHistoryResponse>> getHistory(
            @PathVariable Long warehouseId,
            @PathVariable Long itemId,
            Pageable pageable,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(inventoryService.getHistory(userId, warehouseId, itemId, pageable));
    }

    /** 최대 생산 가능 수량 */
    @GetMapping("/{itemId}/max-producible")
    public ApiResponse<MaxProducibleResponse> calcMaxProducible(
            @PathVariable Long warehouseId,
            @PathVariable Long itemId,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(inventoryService.calcMaxProducible(userId, warehouseId, itemId));
    }
}
