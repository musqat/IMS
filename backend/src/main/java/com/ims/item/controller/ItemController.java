package com.ims.item.controller;

import com.ims.global.common.ApiResponse;
import com.ims.item.dto.request.ItemCreateRequest;
import com.ims.item.dto.response.ItemResponse;
import com.ims.item.service.ItemService;
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
public class ItemController {

    private final ItemService itemService;

    /** 품목 생성 */
    @PostMapping
    public ResponseEntity<ApiResponse<ItemResponse>> createItem(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ItemCreateRequest request
    ) {
        ItemResponse response = itemService.createItem(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /** 품목 전체 조회 */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ItemResponse>>> getItems(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(itemService.getItems(userId)));
    }

    /** 품목 단건 조회 */
    @GetMapping("/{itemId}")
    public ResponseEntity<ApiResponse<ItemResponse>> getItem(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long itemId
    ) {
        return ResponseEntity.ok(ApiResponse.success(itemService.getItem(userId, itemId)));
    }

    /** 품목 삭제 */
    @DeleteMapping("/{itemId}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long itemId
    ) {
        itemService.deleteItem(userId, itemId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
