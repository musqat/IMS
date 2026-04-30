package com.ims.production.repository;

import com.ims.production.entity.ProductionRecord;
import com.ims.production.entity.ProductionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductionRepository extends JpaRepository<ProductionRecord, Long> {
    Page<ProductionRecord> findAllByWarehouseId(Long warehouseId, Pageable pageable);
    List<ProductionRecord> findAllByStatus(ProductionStatus status);
}
