package com.ims.global.support;

import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.item.entity.Item;
import com.ims.item.repository.ItemRepository;
import com.ims.warehouse.entity.Warehouse;
import com.ims.warehouse.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 도메인 소유권 검증 공통 헬퍼
 * - 창고 / 품목 조회 + 소유자 검증을 한 번에 처리
 */
@Component
@RequiredArgsConstructor
public class DomainValidator {

    private final WarehouseRepository warehouseRepository;
    private final ItemRepository itemRepository;

    /**
     * 창고 조회 + 소유자 검증
     */
    public Warehouse getOwnedWarehouse(Long userId, Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ImsException(ErrorCode.WAREHOUSE_NOT_FOUND));
        if (!warehouse.getOwner().getId().equals(userId)) {
            throw new ImsException(ErrorCode.WAREHOUSE_NOT_OWNED);
        }
        return warehouse;
    }

    /**
     * 품목 조회 + 소유자 검증
     */
    public Item getOwnedItem(Long userId, Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ImsException(ErrorCode.ITEM_NOT_FOUND));
        if (!item.getOwner().getId().equals(userId)) {
            throw new ImsException(ErrorCode.ITEM_NOT_OWNED);
        }
        return item;
    }
}
