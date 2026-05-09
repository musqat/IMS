package com.ims.production.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.inventory.entity.Inventory;
import com.ims.global.support.InventoryHistoryWriter;
import com.ims.inventory.entity.InventoryHistoryType;
import com.ims.inventory.repository.InventoryRepository;
import com.ims.item.entity.Item;
import com.ims.item.repository.ItemRepository;
import com.ims.item.service.BomService;
import com.ims.production.entity.ProductionRecord;
import com.ims.production.entity.ProductionStatus;
import com.ims.production.entity.Settlement;
import com.ims.production.entity.SettlementResult;
import com.ims.production.repository.ProductionRepository;
import com.ims.production.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final BomService bomService;
    private final ItemRepository itemRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryHistoryWriter inventoryHistoryWriter;
    private final ProductionRepository productionRepository;
    private final SettlementRepository settlementRepository;
    private final ObjectMapper objectMapper;

    /**
     * 생산 기록 결산 (배치에서 호출)
     * - REQUIRES_NEW: 레코드별 독립 트랜잭션 — 한 건 실패가 다른 결산 롤백하지 않음
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Settlement settle(ProductionRecord record) {
        if (record.getStatus() != ProductionStatus.PENDING) {
            throw new ImsException(ErrorCode.PRODUCTION_ALREADY_SETTLED);
        }

        // BOM 트리 탐색 실패 시(깊이 초과 등) FAILED로 결산 — 배치 무한 재시도 방지
        Map<Long, Long> bom;
        try {
            bom = bomService.getFullBomTree(record.getItem().getId(), record.getItem().getOwner().getId());
        } catch (ImsException e) {
            record.settle();
            productionRepository.save(record);
            String anomalyDetail;
            try {
                anomalyDetail = objectMapper.writeValueAsString(Map.of("error", e.getErrorCode().getMessage()));
            } catch (Exception jsonEx) {
                anomalyDetail = "{\"error\":\"직렬화 실패\"}";
            }
            return settlementRepository.save(Settlement.builder()
                    .productionRecord(record)
                    .result(SettlementResult.FAILED)
                    .anomalyDetail(anomalyDetail)
                    .build());
        }

        Map<String, Object> anomalyMap = new HashMap<>();

        // 1. 부품 재고 비관적 락 일괄 조회 — 동시 결산 시 음수 재고 방지
        List<Long> partIds = new ArrayList<>(bom.keySet());
        Map<Long, Inventory> inventoryMap = inventoryRepository
                .findAllByWarehouseIdAndItemIdInForUpdate(record.getWarehouse().getId(), partIds)
                .stream()
                .collect(Collectors.toMap(inv -> inv.getItem().getId(), Function.identity()));

        // 부품 ID → itemCode 맵
        Map<Long, String> itemCodeMap = itemRepository.findAllById(partIds)
                .stream()
                .collect(Collectors.toMap(Item::getId, Item::getItemCode));

        // 2. 부품별 재고 차감
        for (Map.Entry<Long, Long> entry : bom.entrySet()) {
            Long partId = entry.getKey();
            long required = entry.getValue() * record.getQuantity();
            String itemCode = itemCodeMap.getOrDefault(partId, "ID:" + partId);

            Inventory inventory = inventoryMap.get(partId);

            if (inventory == null) {
                // 재고 항목 자체가 없음
                anomalyMap.put(itemCode, Map.of("required", required, "stock", 0));
                continue;
            }

            int stock = inventory.getQuantity();

            if (stock < required) {
                // 재고 부족 → 가능한 수량만 차감
                // (required > Integer.MAX_VALUE 이면 stock(int) < required 항상 성립 → 이 브랜치에서 처리)
                anomalyMap.put(itemCode, Map.of("required", required, "stock", stock));
                inventory.deduct(stock);
                inventoryHistoryWriter.save(inventory, InventoryHistoryType.PRODUCTION_DEDUCTION, -stock, "생산 결산 차감");
            } else {
                // stock >= required 이고 stock 은 int(≤ MAX_INT) → required ≤ MAX_INT 보장 → (int) 캐스트 안전
                inventory.deduct((int) required);
                inventoryHistoryWriter.save(inventory, InventoryHistoryType.PRODUCTION_DEDUCTION, (int) -required, "생산 결산 차감");
            }
        }

        // 3. 결과 판정
        SettlementResult result = anomalyMap.isEmpty() ? SettlementResult.SUCCESS : SettlementResult.ANOMALY;

        // 4. anomalyDetail 직렬화
        String anomalyDetail = null;
        if (!anomalyMap.isEmpty()) {
            try {
                anomalyDetail = objectMapper.writeValueAsString(anomalyMap);
            } catch (Exception e) {
                log.error("anomalyDetail 직렬화 실패", e);
                anomalyDetail = anomalyMap.toString();
            }
        }

        // 5. 결산 완료 처리
        record.settle();
        productionRepository.save(record);

        return settlementRepository.save(Settlement.builder()
                .productionRecord(record)
                .result(result)
                .anomalyDetail(anomalyDetail)
                .build());
    }

}
