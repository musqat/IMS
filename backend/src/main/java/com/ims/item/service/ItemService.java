package com.ims.item.service;

import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.item.dto.request.ItemCreateRequest;
import com.ims.item.dto.response.ItemResponse;
import com.ims.item.entity.Item;
import com.ims.item.repository.BomRepository;
import com.ims.item.repository.ItemRepository;
import com.ims.user.entity.User;
import com.ims.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BomRepository bomRepository;

    /**
     * 품목 생성
     * - User 조회
     * - itemCode 중복 확인 (owner + itemCode UK) → DUPLICATE_ITEM_CODE
     * - 품목 저장 후 응답 반환
     */
    @Transactional
    public ItemResponse createItem(Long userId, ItemCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ImsException(ErrorCode.USER_NOT_FOUND));

        if (itemRepository.existsByOwnerIdAndItemCode(userId, request.itemCode())) {
            throw new ImsException(ErrorCode.DUPLICATE_ITEM_CODE);
        }

        Item item = Item.builder()
                .owner(user)
                .itemCode(request.itemCode())
                .name(request.name())
                .type(request.type())
                .description(request.description())
                .build();

        return ItemResponse.from(itemRepository.save(item));
    }

    /**
     * 내 회사 품목 전체 조회
     */
    public List<ItemResponse> getItems(Long userId) {
        return itemRepository.findAllByOwnerId(userId).stream().map(ItemResponse::from).toList();
    }

    /**
     * 품목 단건 조회
     * - ITEM_NOT_FOUND, ITEM_NOT_OWNED
     */
    public ItemResponse getItem(Long userId, Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ImsException(ErrorCode.ITEM_NOT_FOUND));

        if (!item.getOwner().getId().equals(userId)) {
            throw new ImsException(ErrorCode.ITEM_NOT_OWNED);
        }

        return ItemResponse.from(item);
    }

    /**
     * 품목 삭제
     * - 소유자 검증 → ITEM_NOT_OWNED
     * - BOM parent/child 참조 확인 → ITEM_IN_USE_BY_BOM
     * - 검증 통과 시 삭제
     */
    @Transactional
    public void deleteItem(Long userId, Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ImsException(ErrorCode.ITEM_NOT_FOUND));

        if (!item.getOwner().getId().equals(userId)) {
            throw new ImsException(ErrorCode.ITEM_NOT_OWNED);
        }

        if (bomRepository.existsByParentId(itemId) || bomRepository.existsByChildId(itemId)) {
            throw new ImsException(ErrorCode.ITEM_IN_USE_BY_BOM);
        }

        itemRepository.delete(item);
    }
}
