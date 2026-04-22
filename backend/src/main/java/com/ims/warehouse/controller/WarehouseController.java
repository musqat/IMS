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

    // ===== 창고 CRUD =====

    @PostMapping
    public ResponseEntity<ApiResponse<WarehouseResponse>> create(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid WarehouseCreateRequest request
    ) {
        WarehouseResponse response = warehouseService.createWarehouse(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WarehouseResponse>>> getList(
            @AuthenticationPrincipal Long userId
    ) {
        List<WarehouseResponse> response = warehouseService.getWarehouses(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{warehouseId}")
    public ResponseEntity<ApiResponse<WarehouseResponse>> getOne(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long warehouseId
    ) {
        WarehouseResponse response = warehouseService.getWarehouse(userId, warehouseId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{warehouseId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long warehouseId
    ) {
        warehouseService.deleteWarehouse(userId, warehouseId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ===== 창고 공유 =====

    @PostMapping("/{warehouseId}/shares")
    public ResponseEntity<ApiResponse<WarehouseShareResponse>> share(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long warehouseId,
            @RequestBody @Valid ShareRequest request
    ) {
        WarehouseShareResponse response = warehouseShareService.share(userId, warehouseId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @DeleteMapping("/{warehouseId}/shares")
    public ResponseEntity<ApiResponse<Void>> revoke(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long warehouseId,
            @RequestParam String companyCode
    ) {
        warehouseShareService.revoke(userId, warehouseId, companyCode);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/shared")
    public ResponseEntity<ApiResponse<List<WarehouseShareResponse>>> getShared(
            @AuthenticationPrincipal Long userId
    ) {
        List<WarehouseShareResponse> response = warehouseShareService.getSharedWarehouses(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
