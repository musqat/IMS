package com.ims.warehouse.repository;

import com.ims.warehouse.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    /** 소유자 창고 목록 전체 조회 (비활성 포함) */
    List<Warehouse> findAllByOwnerId(Long ownerId);

    /**
     * 소유자의 사용 중인 창고만 조회
     * - 목록·선택지에는 닫은 창고가 나오면 안 된다
     */
    List<Warehouse> findAllByOwnerIdAndActiveTrue(Long ownerId);
}
