package com.ims.production.repository;

import com.ims.production.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    Optional<Settlement> findByProductionRecordId(Long productionRecordId);
}
