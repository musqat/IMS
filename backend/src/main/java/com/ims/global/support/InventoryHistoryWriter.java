package com.ims.global.support;

import com.ims.inventory.entity.Inventory;
import com.ims.inventory.entity.InventoryHistory;
import com.ims.inventory.entity.InventoryHistoryType;
import com.ims.inventory.repository.InventoryHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 재고 이력 저장 공통 헬퍼
 * - InventoryHistory 엔티티 빌드 + 저장
 */
@Component
@RequiredArgsConstructor
public class InventoryHistoryWriter {

    private final InventoryHistoryRepository inventoryHistoryRepository;

    /**
     * 재고 이력 저장
     * - MANDATORY: 반드시 활성 트랜잭션 안에서 호출해야 함 (트랜잭션 없으면 즉시 예외)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void save(Inventory inventory, InventoryHistoryType type, int delta, String memo) {
        InventoryHistory history = InventoryHistory.builder()
                .inventory(inventory)
                .type(type)
                .delta(delta)
                .memo(memo)
                .build();
        inventoryHistoryRepository.save(history);
    }
}
