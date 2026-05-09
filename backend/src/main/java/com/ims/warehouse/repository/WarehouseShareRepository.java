package com.ims.warehouse.repository;

import com.ims.warehouse.entity.WarehouseShare;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;

public interface WarehouseShareRepository extends JpaRepository<WarehouseShare, Long> {

    /** 공유받은 창고 목록 전체 조회 (warehouse, owner) */
    @EntityGraph(attributePaths = {"warehouse", "warehouse.owner"})
    List<WarehouseShare> findAllBySharedWithId(Long sharedWithId);

    /** 특정 창고에 대한 특정 User의 공유 권한 조회 */
    Optional<WarehouseShare> findByWarehouseIdAndSharedWithId(Long warehouseId, Long sharedWithId);

    /**
     * 파트너십 해제 시 공유 정리 — warehouse.owner.id + sharedWith.id 기준 삭제
     * PartnershipService에서 양방향(A→B, B→A) 두 번 호출
     */
    @Modifying
    void deleteByWarehouseOwnerIdAndSharedWithId(Long warehouseOwnerId, Long sharedWithId);
}
