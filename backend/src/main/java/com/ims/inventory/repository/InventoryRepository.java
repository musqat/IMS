package com.ims.inventory.repository;

import com.ims.inventory.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long>, JpaSpecificationExecutor<Inventory> {

    /**
     * 재고 목록 조회 — item을 함께 로딩한다.
     * InventoryResponse가 품목 코드·이름·타입을 읽기 때문에, 없으면 행마다 추가 쿼리가 나간다.
     */
    @Override
    @EntityGraph(attributePaths = {"item"})
    Page<Inventory> findAll(Specification<Inventory> spec, Pageable pageable);

    Optional<Inventory> findByWarehouseIdAndItemId(Long warehouseId, Long itemId);

    /** 부품 목록을 한 번에 일괄 조회 */
    List<Inventory> findAllByWarehouseIdAndItemIdIn(Long warehouseId, List<Long> itemIds);

    /**
     * 결산용 비관적 락 일괄 조회
     * - 동시 결산 요청 시 재고 음수 방지
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.warehouse.id = :warehouseId AND i.item.id IN :itemIds")
    List<Inventory> findAllByWarehouseIdAndItemIdInForUpdate(
            @Param("warehouseId") Long warehouseId,
            @Param("itemIds") List<Long> itemIds);

    /** 창고 내 아이템 확인 */
    boolean existsByWarehouseIdAndItemId(Long warehouseId, Long itemId);
}