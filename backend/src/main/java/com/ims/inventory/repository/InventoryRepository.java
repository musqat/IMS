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
     * 창고에 재고가 있는 품목 전체 조회
     * - 창고 소유자와 품목 소유자가 다를 수 있어(유통사 창고에 제조사 완성품)
     *   품목 소유자 기준 조회만으로는 창고의 실제 재고를 알 수 없다
     */
    @EntityGraph(attributePaths = {"item", "item.owner"})
    List<Inventory> findAllByWarehouseId(Long warehouseId);

    /**
     * 결산용 비관적 락 일괄 조회
     * - 동시 결산 요청 시 재고 음수 방지
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.warehouse.id = :warehouseId AND i.item.id IN :itemIds")
    List<Inventory> findAllByWarehouseIdAndItemIdInForUpdate(
            @Param("warehouseId") Long warehouseId,
            @Param("itemIds") List<Long> itemIds);

    /**
     * 재고 변경용 비관적 락 단건 조회
     * - 결산과 같은 PESSIMISTIC_WRITE를 써야 사용자 출고와 자정 결산이 서로를 막아준다
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.warehouse.id = :warehouseId AND i.item.id = :itemId")
    Optional<Inventory> findByWarehouseIdAndItemIdForUpdate(
            @Param("warehouseId") Long warehouseId,
            @Param("itemId") Long itemId);

    /** 창고에 재고 항목이 하나라도 있는지 — 창고 삭제 가능 여부 판단용 */
    boolean existsByWarehouseId(Long warehouseId);

    /** 창고 내 아이템 확인 */
    boolean existsByWarehouseIdAndItemId(Long warehouseId, Long itemId);
}