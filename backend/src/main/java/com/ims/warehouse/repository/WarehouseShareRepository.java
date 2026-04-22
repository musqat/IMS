package com.ims.warehouse.repository;

import com.ims.warehouse.entity.WarehouseShare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WarehouseShareRepository extends JpaRepository<WarehouseShare, Long> {

    // 공유된 창고 목록 조회
    List<WarehouseShare> findAllBySharedWithId(Long sharedWithId);

    // 특정 창고에 대한 특정 User의 공유 권한 조회
    Optional<WarehouseShare> findByWarehouseIdAndSharedWithId(Long warehouseId, Long sharedWithId);
}
