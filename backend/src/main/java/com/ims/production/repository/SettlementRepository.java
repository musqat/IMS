package com.ims.production.repository;

import com.ims.production.entity.Settlement;
import com.ims.production.entity.SettlementResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    Optional<Settlement> findByProductionRecordId(Long productionRecordId);

    /** 페이지 내 레코드 전체를 한 번에 조회 */
    List<Settlement> findAllByProductionRecordIdIn(List<Long> productionRecordIds);

    /** 접근 가능한 창고 ID 목록 기준 결산 결과 건수 */
    long countByResultAndProductionRecordWarehouseIdIn(
            SettlementResult result, Collection<Long> warehouseIds);
}
