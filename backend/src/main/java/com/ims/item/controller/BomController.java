package com.ims.item.controller;

import com.ims.global.common.ApiResponse;
import com.ims.item.dto.request.BomCreateRequest;
import com.ims.item.dto.response.BomResponse;
import com.ims.item.service.BomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
public class BomController {

    private final BomService bomService;

    /** BOM 등록 */
    @PostMapping("/{parentItemId}/bom")
    public ResponseEntity<ApiResponse<BomResponse>> addBom(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long parentItemId,
            @Valid @RequestBody BomCreateRequest request
    ) {
        BomResponse response = bomService.addBom(userId, parentItemId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /** 하위 BOM 목록 조회 */
    @GetMapping("/{itemId}/bom")
    public ResponseEntity<ApiResponse<List<BomResponse>>> getBoms(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long itemId
    ) {
        return ResponseEntity.ok(ApiResponse.success(bomService.getBoms(userId, itemId)));
    }

    /** BOM 삭제 */
    @DeleteMapping("/{parentItemId}/bom/{bomId}")
    public ResponseEntity<ApiResponse<Void>> deleteBom(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long parentItemId,
            @PathVariable Long bomId
    ) {
        bomService.deleteBom(userId, bomId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
