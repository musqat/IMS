package com.ims.production.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ims.inventory.entity.Inventory;
import com.ims.inventory.entity.InventoryHistory;
import com.ims.inventory.entity.InventoryHistoryType;
import com.ims.inventory.repository.InventoryHistoryRepository;
import com.ims.inventory.repository.InventoryRepository;
import com.ims.item.service.BomService;
import com.ims.production.entity.ProductionRecord;
import com.ims.production.entity.Settlement;
import com.ims.production.entity.SettlementResult;
import com.ims.production.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final BomService bomService;
    private final InventoryRepository inventoryRepository;
    private final InventoryHistoryRepository inventoryHistoryRepository;
    private final SettlementRepository settlementRepository;
    private final ObjectMapper objectMapper;

    /**
     * 생산 기록 결산 (배치에서 호출)
     * - bomService.getFullBomTree(record.getItem().getId()) → 부품별 필요 수량 맵
     * - 각 부품에 대해: 필요 수량 = BOM수량 * record.getQuantity()
     * - 재고 조회 → 부족 시 anomalyMap에 기록, 가능한 수량만큼만 차감
     * - 차감 시 InventoryHistory(PRODUCTION_DEDUCTION) 저장
     * - anomalyMap 비어있으면 SUCCESS, 아니면 ANOMALY
     * - record.settle() 호출 후 Settlement 저장 반환
     */
    @Transactional
    public Settlement settle(ProductionRecord record) {
        Map<Long, Integer> bom = bomService.getFullBomTree(record.getItem().getId());
        Map<String, Object> anomalyMap = new HashMap<>();

        // 1. 부품별 재고 차감
        for (Map.Entry<Long, Integer> entry : bom.entrySet()) {
            Long partId = entry.getKey();
            int required = entry.getValue() * record.getQuantity();

            Optional<Inventory> invOpt = inventoryRepository
                    .findByWarehouseIdAndItemId(record.getWarehouse().getId(), partId);

            if (invOpt.isEmpty()) {
                // 재고 항목 자체가 없음
                anomalyMap.put(String.valueOf(partId), Map.of("required", required, "stock", 0));
                continue;
            }

            Inventory inventory = invOpt.get();
            int stock = inventory.getQuantity();

            if (stock < required) {
                // 재고 부족 → 가능한 수량만 차감
                anomalyMap.put(String.valueOf(partId), Map.of("required", required, "stock", stock));
                inventory.deduct(stock);
                saveHistory(inventory, -stock);
            } else {
                inventory.deduct(required);
                saveHistory(inventory, -required);
            }
        }

        // 2. 결과 판정
        SettlementResult result = anomalyMap.isEmpty() ? SettlementResult.SUCCESS : SettlementResult.ANOMALY;

        // 3. anomalyDetail 직렬화
        String anomalyDetail = null;
        if (!anomalyMap.isEmpty()) {
            try {
                anomalyDetail = objectMapper.writeValueAsString(anomalyMap);
            } catch (Exception e) {
                log.error("anomalyDetail 직렬화 실패", e);
                anomalyDetail = anomalyMap.toString();
            }
        }

        // 4. 결산 완료 처리
        record.settle();

        return settlementRepository.save(Settlement.builder()
                .productionRecord(record)
                .result(result)
                .anomalyDetail(anomalyDetail)
                .build());
    }

    /** InventoryHistory(PRODUCTION_DEDUCTION) 저장 헬퍼 */
    private void saveHistory(Inventory inventory, int delta) {
        InventoryHistory history = InventoryHistory
                .builder()
                .inventory(inventory)
                .type(InventoryHistoryType.PRODUCTION_DEDUCTION)
                .delta(delta)
                .memo("생산 결산 차감")
                .build();

        inventoryHistoryRepository.save(history);
    }
}
