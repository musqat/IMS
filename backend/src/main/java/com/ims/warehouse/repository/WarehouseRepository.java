package com.ims.warehouse.repository;

import com.ims.warehouse.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    /** 소유자 창고 목록 전체 조회 */
    List<Warehouse> findAllByOwnerId(Long ownerId);
}
