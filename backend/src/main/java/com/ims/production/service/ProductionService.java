package com.ims.production.service;

import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.global.support.DomainValidator;
import com.ims.item.entity.Item;
import com.ims.production.dto.request.ProductionCreateRequest;
import com.ims.production.dto.response.ProductionCountsResponse;
import com.ims.production.dto.request.ProductionUpdateRequest;
import com.ims.production.dto.request.SettlementUpdateRequest;
import com.ims.production.dto.response.ProductionResponse;
import com.ims.production.dto.response.SettlementResponse;
import com.ims.production.entity.ProductionRecord;
import com.ims.production.entity.ProductionStatus;
import com.ims.production.entity.Settlement;
import com.ims.production.repository.ProductionRepository;
import com.ims.production.repository.SettlementRepository;
import com.ims.warehouse.entity.Warehouse;
import com.ims.warehouse.service.WarehouseShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductionService {

    private final ProductionRepository productionRepository;
    private final SettlementRepository settlementRepository;
    private final SettlementService settlementService;
    private final DomainValidator domainValidator;
    private final WarehouseShareService warehouseShareService;

    /**
     * 생산 기록 등록
     * - 창고 소유자 검증
     * - item 조회 및 소유자 검증
     * - item 타입 검증
     * - ProductionRecord(PENDING) 저장
     */
    @Transactional
    public ProductionResponse createRecord(Long userId, Long warehouseId, ProductionCreateRequest request) {
        Warehouse warehouse = domainValidator.getOwnedWarehouse(userId, warehouseId);
        Item item = domainValidator.getOwnedItem(userId, request.itemId());

        ProductionRecord record = ProductionRecord
                .builder()
                .warehouse(warehouse)
                .item(item)
                .quantity(request.quantity())
                .status(ProductionStatus.PENDING)
                .build();

        return ProductionResponse.from(productionRepository.save(record));
    }

    /**
     * 생산 기록 취소
     * - 창고 소유자 및 레코드 소속 검증
     * - PENDING 상태인 기록만 취소 가능
     */
    @Transactional
    public void cancelRecord(Long userId, Long warehouseId, Long recordId) {
        ProductionRecord record = getOwnedRecord(userId, warehouseId, recordId);
        record.cancel();
    }

    /**
     * 생산 기록 목록 조회
     * - 조회 권한 검증 (소유자 / VIEW / FULL)
     * - Settlement를 한 번에 조회하여 응답 조합
     * - 생산 기록 생성·수정은 소유자 전용이지만 조회는 공유받은 쪽도 가능해야 한다.
     *   본사가 하청 창고의 생산 현황을 확인하는 것이 창고 공유의 목적이다
     */
    public Page<ProductionResponse> getRecords(Long userId, Long warehouseId, Pageable pageable) {
        warehouseShareService.checkViewAccess(userId, warehouseId);
        Page<ProductionRecord> page = productionRepository.findAllByWarehouseId(warehouseId, pageable);

        List<Long> recordIds = page.getContent().stream().map(ProductionRecord::getId).toList();
        if (recordIds.isEmpty()) {
            return page.map(record -> ProductionResponse.from(record, null));
        }
        Map<Long, Settlement> settlementMap = settlementRepository.findAllByProductionRecordIdIn(recordIds)
                .stream().collect(Collectors.toMap(s -> s.getProductionRecord().getId(), s -> s));

        return page.map(record -> ProductionResponse.from(record,
                Optional.ofNullable(settlementMap.get(record.getId()))
                        .map(SettlementResponse::from).orElse(null)));
    }

    /**
     * 상태 필터 + 페이지네이션 — 생산 탭 테이블용
     */
    public Page<ProductionResponse> getRecordsByStatus(Long userId, ProductionStatus status, Pageable pageable) {
        List<Long> warehouseIds = warehouseShareService.getAccessibleWarehouseIds(userId);
        Page<ProductionRecord> page = productionRepository.findAllByWarehouseIdInAndStatus(warehouseIds, status, pageable);

        List<Long> recordIds = page.getContent().stream().map(ProductionRecord::getId).toList();
        if (recordIds.isEmpty()) {
            return page.map(record -> ProductionResponse.from(record, null));
        }
        Map<Long, Settlement> settlementMap = settlementRepository.findAllByProductionRecordIdIn(recordIds)
                .stream().collect(Collectors.toMap(s -> s.getProductionRecord().getId(), s -> s));

        return page.map(record -> ProductionResponse.from(record,
                Optional.ofNullable(settlementMap.get(record.getId()))
                        .map(SettlementResponse::from).orElse(null)));
    }

    /**
     * 상태별 + ANOMALY 건수 집계 — 대시보드 KPI / 생산 탭 뱃지용
     */
    public ProductionCountsResponse getStatusCounts(Long userId) {
        List<Long> warehouseIds = warehouseShareService.getAccessibleWarehouseIds(userId);

        Map<ProductionStatus, Long> statusMap = productionRepository
                .countGroupByStatusInWarehouses(warehouseIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (ProductionStatus) row[0],
                        row -> (Long) row[1]
                ));

        long anomaly = settlementRepository.countByResultAndProductionRecordWarehouseIdIn(
                com.ims.production.entity.SettlementResult.ANOMALY, warehouseIds);

        return ProductionCountsResponse.of(
                statusMap.getOrDefault(ProductionStatus.PENDING, 0L),
                statusMap.getOrDefault(ProductionStatus.SETTLED, 0L),
                statusMap.getOrDefault(ProductionStatus.CANCELLED, 0L),
                anomaly
        );
    }

    /**
     * 생산 기록 수정 (수량)
     * - 창고 소유자 및 레코드 소속 검증
     * - PENDING 상태인 기록만 수정 가능
     */
    @Transactional
    public ProductionResponse updateRecord(Long userId, Long warehouseId, Long recordId, ProductionUpdateRequest request) {
        ProductionRecord record = getOwnedRecord(userId, warehouseId, recordId);
        record.updateQuantity(request.quantity());
        return ProductionResponse.from(record);
    }

    /**
     * 결산 결과 수동 수정
     * - 창고 소유자 및 레코드 소속 검증
     * - SETTLED 상태인 기록의 결산만 수정 가능
     */
    @Transactional
    public ProductionResponse updateSettlement(Long userId, Long warehouseId, Long recordId, SettlementUpdateRequest request) {
        ProductionRecord record = getOwnedRecord(userId, warehouseId, recordId);
        if (!record.getStatus().equals(ProductionStatus.SETTLED)) {
            throw new ImsException(ErrorCode.PRODUCTION_NOT_SETTLED);
        }

        Settlement settlement = settlementRepository.findByProductionRecordId(recordId)
                .orElseThrow(() -> new ImsException(ErrorCode.SETTLEMENT_NOT_FOUND));
        settlement.update(request.result(), request.memo());
        return ProductionResponse.from(record, SettlementResponse.from(settlement));
    }

    /**
     * 강제 결산 (즉시 결산)
     * - 창고 소유자 및 레코드 소속 검증
     * - PENDING 상태인 기록만 즉시 결산 가능
     * - 실제 쓰기는 settlementService.settle() 의 REQUIRES_NEW 트랜잭션 안에서 커밋됨
     *   읽기 전용 트랜잭션에서 레코드 조회 후 settle() 위임
     */
    public ProductionResponse forceSettle(Long userId, Long warehouseId, Long recordId) {
        domainValidator.getOwnedWarehouse(userId, warehouseId);
        // REQUIRES_NEW 진입 전 item.owner까지 eager fetch — LazyInitializationException 방지
        ProductionRecord record = productionRepository.findWithDetailsById(recordId)
                .orElseThrow(() -> new ImsException(ErrorCode.PRODUCTION_NOT_FOUND));
        if (!record.getWarehouse().getId().equals(warehouseId)) {
            throw new ImsException(ErrorCode.WAREHOUSE_NOT_OWNED);
        }
        if (!record.getStatus().equals(ProductionStatus.PENDING)) {
            throw new ImsException(ErrorCode.PRODUCTION_NOT_MODIFIABLE);
        }

        Settlement settlement = settlementService.settle(record);
        // REQUIRES_NEW 트랜잭션에서 record.settle()이 커밋됐으므로 DB에서 재조회하여 최신 status 반영
        ProductionRecord settled = productionRepository.findById(recordId)
                .orElseThrow(() -> new ImsException(ErrorCode.PRODUCTION_NOT_FOUND));
        return ProductionResponse.from(settled, SettlementResponse.from(settlement));
    }

    //======================== 헬퍼 메소드 ===========================//

    /**
     * 창고 소유권, 레코드 조회, 레코드-창고 소속 검증을 한 번에 처리하는 헬퍼
     */
    private ProductionRecord getOwnedRecord(Long userId, Long warehouseId, Long recordId) {
        domainValidator.getOwnedWarehouse(userId, warehouseId);
        ProductionRecord record = productionRepository.findById(recordId)
                .orElseThrow(() -> new ImsException(ErrorCode.PRODUCTION_NOT_FOUND));
        if (!record.getWarehouse().getId().equals(warehouseId)) {
            throw new ImsException(ErrorCode.WAREHOUSE_NOT_OWNED);
        }
        return record;
    }
}
