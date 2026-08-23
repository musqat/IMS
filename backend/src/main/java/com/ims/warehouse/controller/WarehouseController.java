package com.ims.warehouse.controller;

import com.ims.global.common.ApiResponse;
import com.ims.warehouse.dto.request.ShareRequest;
import com.ims.warehouse.dto.request.WarehouseCreateRequest;
import com.ims.warehouse.dto.response.WarehouseResponse;
import com.ims.warehouse.dto.response.WarehouseShareResponse;
import com.ims.warehouse.service.WarehouseService;
import com.ims.warehouse.service.WarehouseShareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/warehouses")
public class WarehouseController {

    private final WarehouseService warehouseService;
    private final WarehouseShareService warehouseShareService;

    /** 창고 생성 */
    @PostMapping
    public ResponseEntity<ApiResponse<WarehouseResponse>> create(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid WarehouseCreateRequest request
    ) {
        WarehouseResponse response = warehouseService.createWarehouse(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /** 내 창고 전체 조회 */
    @GetMapping
    public ResponseEntity<ApiResponse<List<WarehouseResponse>>> getList(
            @AuthenticationPrincipal Long userId
    ) {
        List<WarehouseResponse> response = warehouseService.getWarehouses(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** 창고 단건 조회 */
    @GetMapping("/{warehouseId}")
    public ResponseEntity<ApiResponse<WarehouseResponse>> getOne(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long warehouseId
    ) {
        WarehouseResponse response = warehouseService.getWarehouse(userId, warehouseId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** 창고 삭제 */
    @DeleteMapping("/{warehouseId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long warehouseId
    ) {
        warehouseService.deleteWarehouse(userId, warehouseId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /** 비활성 창고 목록 — 활성화용 */
    @GetMapping("/inactive")
    public ResponseEntity<ApiResponse<List<WarehouseResponse>>> getInactive(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(warehouseService.getInactiveWarehouses(userId)));
    }

    /** 창고 비활성화 (소프트 삭제) — 목록·쓰기에서 제외하고 이력은 보존한다 */
    @PatchMapping("/{warehouseId}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long warehouseId
    ) {
        warehouseService.deactivateWarehouse(userId, warehouseId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /** 비활성 창고 활성화 */
    @PatchMapping("/{warehouseId}/activate")
    public ResponseEntity<ApiResponse<Void>> activate(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long warehouseId
    ) {
        warehouseService.activateWarehouse(userId, warehouseId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /** 창고 공유 부여 */
    @PostMapping("/{warehouseId}/shares")
    public ResponseEntity<ApiResponse<WarehouseShareResponse>> share(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long warehouseId,
            @RequestBody @Valid ShareRequest request
    ) {
        WarehouseShareResponse response = warehouseShareService.share(userId, warehouseId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /** 창고 공유 회수 */
    @DeleteMapping("/{warehouseId}/shares")
    public ResponseEntity<ApiResponse<Void>> revoke(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long warehouseId,
            @RequestParam String companyCode
    ) {
        warehouseShareService.revoke(userId, warehouseId, companyCode);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /** 공유받은 창고 목록 조회 */
    @GetMapping("/shared")
    public ResponseEntity<ApiResponse<List<WarehouseShareResponse>>> getShared(
            @AuthenticationPrincipal Long userId
    ) {
        List<WarehouseShareResponse> response = warehouseShareService.getSharedWarehouses(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
