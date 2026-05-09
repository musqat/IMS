package com.ims.inventory.repository;

import com.ims.inventory.entity.InventoryHistory;
import com.ims.inventory.entity.InventoryHistoryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface InventoryHistoryRepository extends JpaRepository<InventoryHistory, Long> {

    Page<InventoryHistory> findAllByInventoryId(Long inventoryId, Pageable pageable);

    /** 창고 전체 이력을 타입·기간으로 조회 */
    @EntityGraph(attributePaths = {"inventory", "inventory.item"})
    List<InventoryHistory> findAllByInventory_WarehouseIdAndTypeInAndCreatedAtBetween(
            Long warehouseId,
            List<InventoryHistoryType> types,
            LocalDateTime from,
            LocalDateTime to
    );
}
