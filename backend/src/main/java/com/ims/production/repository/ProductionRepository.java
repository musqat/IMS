package com.ims.production.repository;

import com.ims.production.entity.ProductionRecord;
import com.ims.production.entity.ProductionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductionRepository extends JpaRepository<ProductionRecord, Long> {

    /**
     * 상태별 카운트 집계 — 접근 가능한 창고 ID 목록 기준 GROUP BY
     * Object[0] = ProductionStatus, Object[1] = Long
     */
    @Query("SELECT p.status, COUNT(p) FROM ProductionRecord p WHERE p.warehouse.id IN :warehouseIds GROUP BY p.status")
    List<Object[]> countGroupByStatusInWarehouses(@Param("warehouseIds") Collection<Long> warehouseIds);

    /** 상태 필터 + 페이지네이션 — 접근 가능한 창고 ID 목록 기준 */
    @EntityGraph(attributePaths = {"warehouse", "item"})
    Page<ProductionRecord> findAllByWarehouseIdInAndStatus(
            Collection<Long> warehouseIds, ProductionStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"warehouse", "item"})
    Page<ProductionRecord> findAllByWarehouseId(Long warehouseId, Pageable pageable);

    /** 자정 배치용 — warehouse / item / item.owner 를 한 번에 페치하여 N+1 방지 */
    @EntityGraph(attributePaths = {"warehouse", "item", "item.owner"})
    List<ProductionRecord> findAllByStatus(ProductionStatus status);

    /** 강제결산용 — LAZY 연관관계 선제적 로딩 (REQUIRES_NEW 트랜잭션 진입 전 초기화) */
    @EntityGraph(attributePaths = {"warehouse", "item", "item.owner"})
    Optional<ProductionRecord> findWithDetailsById(Long id);

    /** 창고에 생산 기록이 하나라도 있는지 — 창고 삭제 가능 여부 판단용 */
    boolean existsByWarehouseId(Long warehouseId);
}
