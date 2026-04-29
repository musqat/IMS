package com.ims.inventory.repository;

import com.ims.inventory.entity.InventoryHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryHistoryRepository extends JpaRepository<InventoryHistory, Long> {

    Page<InventoryHistory> findAllByInventoryId(Long inventoryId, Pageable pageable);
}
