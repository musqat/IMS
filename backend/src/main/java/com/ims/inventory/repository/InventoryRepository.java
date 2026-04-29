package com.ims.inventory.repository;

import com.ims.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long>, JpaSpecificationExecutor<Inventory> {
    Optional<Inventory> findByWarehouseIdAndItemId(Long warehouseId, Long itemId);

    boolean existsByWarehouseIdAndItemId(Long warehouseId, Long itemId);
}