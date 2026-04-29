package com.ims.inventory.service;

import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.inventory.dto.request.InventoryCreateRequest;
import com.ims.inventory.dto.request.AdjustRequest;
import com.ims.inventory.dto.request.InboundRequest;
import com.ims.inventory.dto.request.OutboundRequest;
import com.ims.inventory.dto.response.InventoryHistoryResponse;
import com.ims.inventory.dto.response.InventoryResponse;
import com.ims.inventory.dto.response.MaxProducibleResponse;
import com.ims.inventory.entity.Inventory;
import com.ims.inventory.entity.InventoryHistory;
import com.ims.inventory.entity.InventoryHistoryType;
import com.ims.inventory.repository.InventoryHistoryRepository;
import com.ims.inventory.repository.InventoryRepository;
import com.ims.item.entity.Item;
import com.ims.item.repository.ItemRepository;
import com.ims.item.service.BomService;
import com.ims.warehouse.entity.Warehouse;
import com.ims.warehouse.repository.WarehouseRepository;
import com.ims.warehouse.service.WarehouseShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryHistoryRepository inventoryHistoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final ItemRepository itemRepository;
    private final WarehouseShareService warehouseShareService;
    private final BomService bomService;

    /**
     * 재고 항목 등록 (창고 + 품목 조합 최초 1회)
     * - 창고 소유자 검증, 품목 존재 검증, 중복 검증
     * - quantity=0 으로 초기화 후 저장
     */
    @Transactional
    public InventoryResponse createInventory(Long userId, Long warehouseId, InventoryCreateRequest request) {
        Warehouse warehouse = getOwnedWarehouse(userId, warehouseId);
        Item item = itemRepository.findById(request.itemId()).orElseThrow(() -> new ImsException(ErrorCode.ITEM_NOT_FOUND));
        if (inventoryRepository.existsByWarehouseIdAndItemId(warehouseId, item.getId())) {
            throw new ImsException(ErrorCode.DUPLICATE_INVENTORY);
        }

        Inventory inventory = Inventory.builder()
                .warehouse(warehouse)
                .item(item)
                .quantity(0)
                .safetyStock(request.safetyStock())
                .build();

        return InventoryResponse.from(inventoryRepository.save(inventory));
    }

    /**
     * 입고
     * - 소유자 검증 후 quantity 증가
     * - History(IN, delta=+qty) 기록
     */
    @Transactional
    public InventoryResponse adjustIn(Long userId, Long warehouseId, Long itemId, InboundRequest request) {
        getOwnedWarehouse(userId, warehouseId);
        Inventory inventory = getInventoryOrThrow(warehouseId, itemId);
        inventory.add(request.quantity());
        saveHistory(inventory, InventoryHistoryType.IN, +request.quantity(), request.memo());
        return InventoryResponse.from(inventory);
    }

    /**
     * 출고
     * - 소유자 검증 후 quantity 차감 (재고 부족 시 INSUFFICIENT_STOCK)
     * - History(OUT, delta=-qty) 기록
     */
    @Transactional
    public InventoryResponse adjustOut(Long userId, Long warehouseId, Long itemId, OutboundRequest request) {
        getOwnedWarehouse(userId, warehouseId);
        Inventory inventory = getInventoryOrThrow(warehouseId, itemId);
        inventory.deduct(request.quantity());
        int delta = -request.quantity();
        saveHistory(inventory, InventoryHistoryType.OUT, delta, request.memo());
        return InventoryResponse.from(inventory);
    }

    /**
     * 재고 실사 보정 (절대값으로 덮어씀)
     * - 소유자 검증 후 newQuantity 로 설정
     * - History(ADJUSTMENT, delta=newQty-oldQty) 기록
     */
    @Transactional
    public InventoryResponse adjust(Long userId, Long warehouseId, Long itemId, AdjustRequest request) {
        getOwnedWarehouse(userId, warehouseId);
        Inventory inventory = getInventoryOrThrow(warehouseId, itemId);
        int delta = request.newQuantity() - inventory.getQuantity(); // 기록용
        inventory.setQuantity(request.newQuantity());
        saveHistory(inventory, InventoryHistoryType.ADJUSTMENT, delta, request.memo());
        return InventoryResponse.from(inventory);
    }

    /**
     * 재고 목록 조회 (페이지네이션 + keyword 검색)
     * - 조회 권한 검증 (소유자 / VIEW / FULL)
     * - keyword 로 itemCode, itemName 검색 (null 허용)
     */
    public Page<InventoryResponse> getInventories(Long userId, Long warehouseId, String keyword, Pageable pageable) {
        warehouseShareService.checkViewAccess(userId, warehouseId);

        Specification<Inventory> spec = (root, query, cb) -> {
            var warehouseCond = cb.equal(root.get("warehouse").get("id"), warehouseId);

            if (keyword == null || keyword.isBlank()) {
                return warehouseCond;
            }

            String pattern = "%" + keyword.toLowerCase() + "%";
            var codeCond = cb.like(cb.lower(root.get("item").get("itemCode")), pattern);
            var nameCond = cb.like(cb.lower(root.get("item").get("name")), pattern);

            return cb.and(warehouseCond, cb.or(codeCond, nameCond));
        };

        return inventoryRepository.findAll(spec, pageable).map(InventoryResponse::from);
    }

    /**
     * 입출고 이력 조회
     * - 조회 권한 검증 후 inventory 조회
     * - 이력 페이지 반환
     */
    public Page<InventoryHistoryResponse> getHistory(Long userId, Long warehouseId, Long itemId, Pageable pageable) {
        warehouseShareService.checkViewAccess(userId, warehouseId);
        Inventory inventory = getInventoryOrThrow(warehouseId, itemId);
        return inventoryHistoryRepository.findAllByInventoryId(inventory.getId(), pageable)
                .map(InventoryHistoryResponse::from);
    }

    /**
     * 최대 생산 가능 수량 계산
     * - BOM 트리 전체 탐색 후 부품별 재고 조회
     * - min(재고 / 필요수량) 반환, BOM 없거나 재고 없으면 0
     */
    public MaxProducibleResponse calcMaxProducible(Long userId, Long warehouseId, Long itemId) {
        warehouseShareService.checkViewAccess(userId, warehouseId);
        Item item = itemRepository.findById(itemId).orElseThrow(() -> new ImsException(ErrorCode.ITEM_NOT_FOUND));
        Map<Long, Integer> requirements = bomService.getFullBomTree(itemId);
        if (requirements.isEmpty()) {
            return new MaxProducibleResponse(itemId, item.getName(), 0);
        }

        int maxQuantity = Integer.MAX_VALUE;
        for (Map.Entry<Long, Integer> entry : requirements.entrySet()) {
            int stock = inventoryRepository.findByWarehouseIdAndItemId(warehouseId, entry.getKey())
                    .map(Inventory::getQuantity).orElse(0);
            if (stock == 0) {
                maxQuantity = 0;
                break;
            }
            maxQuantity = Math.min(maxQuantity, stock / entry.getValue());
        }
        return new MaxProducibleResponse(itemId, item.getName(), maxQuantity == Integer.MAX_VALUE ? 0 : maxQuantity);
    }

    //======================== 헬퍼 메소드 ===========================//

    /**
     * 창고 조회 + 소유자 검증
     * - WAREHOUSE_NOT_FOUND, WAREHOUSE_NOT_OWNED
     */
    private Warehouse getOwnedWarehouse(Long userId, Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId).orElseThrow(() -> new ImsException(ErrorCode.WAREHOUSE_NOT_FOUND));
        if (!warehouse.getOwner().getId().equals(userId)) {
            throw new ImsException(ErrorCode.WAREHOUSE_NOT_OWNED);
        }
        return warehouse;
    }

    /** 창고 + 품목으로 재고 조회 → INVENTORY_NOT_FOUND */
    private Inventory getInventoryOrThrow(Long warehouseId, Long itemId) {
        return inventoryRepository.findByWarehouseIdAndItemId(warehouseId, itemId)
                .orElseThrow(() -> new ImsException(ErrorCode.INVENTORY_NOT_FOUND));
    }

    /** InventoryHistory 저장 */
    private void saveHistory(Inventory inventory, InventoryHistoryType type, int delta, String memo) {
        InventoryHistory history = InventoryHistory.builder()
                .inventory(inventory)
                .type(type)
                .delta(delta)
                .memo(memo)
                .build();
        inventoryHistoryRepository.save(history);
    }
}
