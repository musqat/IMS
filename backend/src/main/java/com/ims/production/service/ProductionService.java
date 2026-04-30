package com.ims.production.service;

import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.item.entity.Item;
import com.ims.item.entity.ItemType;
import com.ims.item.repository.ItemRepository;
import com.ims.production.dto.request.ProductionCreateRequest;
import com.ims.production.dto.response.ProductionResponse;
import com.ims.production.dto.response.SettlementResponse;
import com.ims.production.entity.ProductionRecord;
import com.ims.production.entity.ProductionStatus;
import com.ims.production.entity.Settlement;
import com.ims.production.repository.ProductionRepository;
import com.ims.production.repository.SettlementRepository;
import com.ims.warehouse.entity.Warehouse;
import com.ims.warehouse.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductionService {

    private final ProductionRepository productionRepository;
    private final SettlementRepository settlementRepository;
    private final WarehouseRepository warehouseRepository;
    private final ItemRepository itemRepository;

    /**
     * 생산 기록 등록
     * - 창고 소유자 검증 → WAREHOUSE_NOT_OWNED
     * - item 조회 → ITEM_NOT_FOUND
     * - item 타입 검증 → ITEM_NOT_FINISHED (PRODUCT 아니면 예외)
     * - ProductionRecord(PENDING) 저장 후 응답 반환
     */
    @Transactional
    public ProductionResponse createRecord(Long userId, Long warehouseId, ProductionCreateRequest request) {
        Warehouse warehouse = getOwnedWarehouse(userId, warehouseId);
        Item item = itemRepository.findById(request.itemId()).orElseThrow(() -> new ImsException(ErrorCode.ITEM_NOT_FOUND));
        if (item.getType() != ItemType.PRODUCT) {
            throw new ImsException(ErrorCode.ITEM_NOT_FINISHED);
        }

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
     * - 기록 조회 → PRODUCTION_NOT_FOUND
     * - 소유자 검증 → WAREHOUSE_NOT_OWNED
     * - record.cancel() 호출 (내부에서 PENDING 검증)
     */
    @Transactional
    public void cancelRecord(Long userId, Long recordId) {
        ProductionRecord record = productionRepository.findById(recordId)
                .orElseThrow(() -> new ImsException(ErrorCode.PRODUCTION_NOT_FOUND));
        if (!record.getWarehouse().getOwner().getId().equals(userId)) {
            throw new ImsException(ErrorCode.WAREHOUSE_NOT_OWNED);
        }
        record.cancel();
    }

    /**
     * 생산 기록 목록 조회
     * - 창고 소유자 검증
     * - 각 record에 settlement 조회 후 ProductionResponse.from(record, settlementRes ponse) 반환
     */
    public Page<ProductionResponse> getRecords(Long userId, Long warehouseId, Pageable pageable) {
        getOwnedWarehouse(userId, warehouseId);
        return productionRepository.findAllByWarehouseId(warehouseId, pageable).map(record -> {
            Optional<Settlement> settlement = settlementRepository.findByProductionRecordId(record.getId());
            return ProductionResponse.from(record, settlement.map(SettlementResponse::from).orElse(null));
        });
    }

    /** 창고 조회 + 소유자 검증 */
    private Warehouse getOwnedWarehouse(Long userId, Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ImsException(ErrorCode.WAREHOUSE_NOT_FOUND));
        if (!warehouse.getOwner().getId().equals(userId)) {
            throw new ImsException(ErrorCode.WAREHOUSE_NOT_OWNED);
        }
        return warehouse;
    }
}
