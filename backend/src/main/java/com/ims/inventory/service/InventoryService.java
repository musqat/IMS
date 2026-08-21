package com.ims.inventory.service;

import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.global.support.DomainValidator;
import com.ims.global.support.InventoryHistoryWriter;
import com.ims.inventory.dto.request.InventoryCreateRequest;
import com.ims.inventory.dto.request.AdjustRequest;
import com.ims.inventory.dto.request.InboundRequest;
import com.ims.inventory.dto.request.OutboundRequest;
import com.ims.inventory.dto.request.SafetyStockUpdateRequest;
import com.ims.inventory.dto.response.InventoryExportRow;
import com.ims.inventory.dto.response.InventoryHistoryResponse;
import com.ims.inventory.dto.response.InventoryResponse;
import com.ims.inventory.dto.response.MaxProducibleResponse;
import com.ims.inventory.entity.Inventory;
import com.ims.inventory.entity.InventoryHistoryType;
import com.ims.inventory.repository.InventoryHistoryRepository;
import com.ims.inventory.repository.InventoryRepository;
import com.ims.inventory.dto.response.PartShortageDto;
import com.ims.inventory.dto.response.ShortageItemResponse;
import com.ims.item.entity.Item;
import com.ims.item.entity.ItemType;
import com.ims.item.repository.ItemRepository;
import com.ims.item.service.BomService;
import com.ims.warehouse.entity.Warehouse;
import com.ims.warehouse.service.WarehouseShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryHistoryRepository inventoryHistoryRepository;
    private final WarehouseShareService warehouseShareService;
    private final BomService bomService;
    private final DomainValidator domainValidator;
    private final InventoryHistoryWriter inventoryHistoryWriter;
    private final ItemRepository itemRepository;

    /**
     * 재고 항목 등록 (창고 + 품목 조합 최초 1회)
     * - 창고 소유자 검증, 품목 소유자 검증, 중복 검증
     * - quantity / safetyStock 초기값으로 저장
     */
    @Transactional
    public InventoryResponse createInventory(Long userId, Long warehouseId, InventoryCreateRequest request) {
        Warehouse warehouse = domainValidator.getOwnedWarehouse(userId, warehouseId);
        Item item = domainValidator.getOwnedItem(userId, request.itemId());

        if (inventoryRepository.existsByWarehouseIdAndItemId(warehouseId, item.getId())) {
            throw new ImsException(ErrorCode.DUPLICATE_INVENTORY);
        }

        Inventory inventory = Inventory.builder()
                .warehouse(warehouse)
                .item(item)
                .quantity(request.quantity())
                .safetyStock(request.safetyStock())
                .build();

        return InventoryResponse.from(inventoryRepository.save(inventory));
    }

    /**
     * 입고
     * - 쓰기 권한 검증(소유자 또는 FULL 공유) 후 quantity 증가
     * - History(IN, delta=+qty) 기록
     */
    @Transactional
    public InventoryResponse adjustIn(Long userId, Long warehouseId, Long itemId, InboundRequest request) {
        warehouseShareService.checkFullAccess(userId, warehouseId);
        Inventory inventory = getInventoryOrThrow(warehouseId, itemId);
        inventory.add(request.quantity());
        inventoryHistoryWriter.save(inventory, InventoryHistoryType.IN, +request.quantity(), request.memo());
        return InventoryResponse.from(inventory);
    }

    /**
     * 출고
     * - 쓰기 권한 검증(소유자 또는 FULL 공유) 후 quantity 차감 (재고 부족 시 예외)
     * - History(OUT, delta=-qty) 기록
     */
    @Transactional
    public InventoryResponse adjustOut(Long userId, Long warehouseId, Long itemId, OutboundRequest request) {
        warehouseShareService.checkFullAccess(userId, warehouseId);
        Inventory inventory = getInventoryOrThrow(warehouseId, itemId);
        inventory.deduct(request.quantity());
        inventoryHistoryWriter.save(inventory, InventoryHistoryType.OUT, -request.quantity(), request.memo());
        return InventoryResponse.from(inventory);
    }

    /**
     * 재고 실사 보정 (절대값으로 덮어씀)
     * - 쓰기 권한 검증(소유자 또는 FULL 공유) 후 newQuantity 로 설정
     * - History(ADJUSTMENT, delta=newQty-oldQty) 기록
     */
    @Transactional
    public InventoryResponse adjust(Long userId, Long warehouseId, Long itemId, AdjustRequest request) {
        warehouseShareService.checkFullAccess(userId, warehouseId);
        Inventory inventory = getInventoryOrThrow(warehouseId, itemId);
        int delta = request.quantity() - inventory.getQuantity();
        inventory.setQuantity(request.quantity());
        inventoryHistoryWriter.save(inventory, InventoryHistoryType.ADJUSTMENT, delta, request.memo());
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
     * - 품목은 창고 재고에서 해석하고 BOM은 그 품목의 소유자 기준으로 조회한다.
     *   창고 소유자를 기준으로 삼으면 안 된다. 물류 창고처럼 남의 품목을
     *   보관하기만 하는 창고에서는 창고 소유자와 품목 소유자가 늘 다르다
     */
    public MaxProducibleResponse calcMaxProducible(Long userId, Long warehouseId, Long itemId) {
        warehouseShareService.checkViewAccess(userId, warehouseId);

        Item item = resolveWarehouseItem(userId, warehouseId, itemId);
        // BOM은 품목 소유자가 등록한다. 창고 소유자 기준으로 찾으면
        // 남의 창고에 보관된 내 품목의 BOM을 찾지 못한다
        Long ownerId = item.getOwner().getId();
        Map<Long, Long> requirements = bomService.getFullBomTree(itemId, ownerId);
        if (requirements.isEmpty()) {
            // BOM 없음 = 차감할 부품이 없으므로 제한 없음(null)
            return new MaxProducibleResponse(itemId, item.getName(), null);
        }

        // 부품 재고 일괄 조회
        List<Long> partIds = List.copyOf(requirements.keySet());
        Map<Long, Integer> stockMap = inventoryRepository
                .findAllByWarehouseIdAndItemIdIn(warehouseId, partIds)
                .stream()
                .collect(Collectors.toMap(inv -> inv.getItem().getId(), Inventory::getQuantity));

        long maxQuantity = Long.MAX_VALUE;
        for (Map.Entry<Long, Long> entry : requirements.entrySet()) {
            int stock = stockMap.getOrDefault(entry.getKey(), 0);
            if (stock == 0) {
                return new MaxProducibleResponse(itemId, item.getName(), 0);
            }
            maxQuantity = Math.min(maxQuantity, (long) stock / entry.getValue());
        }
        // int 범위 초과 방지 cap
        return new MaxProducibleResponse(itemId, item.getName(),
                (int) Math.min(maxQuantity, Integer.MAX_VALUE));
    }

    /**
     * 생산 불가 완제품 분석
     * - 1개도 생산 불가(재고 < 필요량)인 부품이 있는 완성품만 반환
     * - 완성품 수와 무관하게 쿼리는 4회로 고정된다
     *   (완성품 목록 / BOM 인접 리스트 / 부품 재고 / 부족 부품 정보)
     */
    public List<ShortageItemResponse> getShortageAnalysis(Long userId, Long warehouseId) {
        Warehouse warehouse = warehouseShareService.checkViewAccess(userId, warehouseId);

        // 분석 대상은 창고 소유자의 완성품과 이 창고에 실제로 재고가 있는 완성품의 합집합이다.
        // 소유자 기준만 쓰면 유통사 창고처럼 남의 완성품을 보관하는 창고에서
        // 조용히 0건이 나온다. 재고 기준만 쓰면 아직 입고하지 않은 완성품이 빠진다
        List<Item> products = collectAnalysisTargets(warehouse, warehouseId);
        if (products.isEmpty()) return List.of();

        // BOM은 품목 소유자가 등록하므로 소유자별로 나눠 조회한다.
        // 대개 한 소유자뿐이라 쿼리 수는 사실상 그대로다
        Map<Long, Map<Long, Long>> allBomTrees = new HashMap<>();
        products.stream()
                .collect(Collectors.groupingBy(item -> item.getOwner().getId(),
                        Collectors.mapping(Item::getId, Collectors.toList())))
                .forEach((itemOwnerId, ids) -> allBomTrees.putAll(bomService.getFullBomTrees(ids, itemOwnerId)));

        // 모든 완성품의 부품을 한 번에 모아 재고를 1회만 조회한다.
        // 제품별로 조회하면 완성품 수만큼 쿼리가 나간다.
        Set<Long> allPartIds = allBomTrees.values().stream()
                .flatMap(requirements -> requirements.keySet().stream())
                .collect(Collectors.toSet());
        if (allPartIds.isEmpty()) return List.of();

        Map<Long, Integer> stockMap = inventoryRepository
                .findAllByWarehouseIdAndItemIdIn(warehouseId, List.copyOf(allPartIds))
                .stream()
                .collect(Collectors.toMap(inv -> inv.getItem().getId(), Inventory::getQuantity));

        // 부족 부품도 전체를 모은 뒤 1회만 조회한다
        Set<Long> allShortagePartIds = allBomTrees.values().stream()
                .flatMap(requirements -> requirements.entrySet().stream())
                .filter(e -> stockMap.getOrDefault(e.getKey(), 0) < e.getValue())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        if (allShortagePartIds.isEmpty()) return List.of();

        Map<Long, Item> partItemMap = itemRepository.findAllById(allShortagePartIds)
                .stream().collect(Collectors.toMap(Item::getId, i -> i));

        return products.stream()
                .map(product -> {
                    Map<Long, Long> requirements = allBomTrees.getOrDefault(product.getId(), Map.of());
                    if (requirements.isEmpty()) return null;

                    Set<Long> shortagePartIds = requirements.entrySet().stream()
                            .filter(e -> stockMap.getOrDefault(e.getKey(), 0) < e.getValue())
                            .map(Map.Entry::getKey)
                            .collect(Collectors.toSet());
                    if (shortagePartIds.isEmpty()) return null;

                    List<PartShortageDto> shortages = requirements.entrySet().stream()
                            .filter(e -> shortagePartIds.contains(e.getKey()))
                            .map(e -> {
                                Item part = partItemMap.get(e.getKey());
                                if (part == null) throw new ImsException(ErrorCode.ITEM_NOT_FOUND);
                                return new PartShortageDto(
                                        e.getKey(),
                                        part.getItemCode(),
                                        part.getName(),
                                        e.getValue(),
                                        stockMap.getOrDefault(e.getKey(), 0)
                                );
                            })
                            .toList();

                    return new ShortageItemResponse(product.getId(), product.getItemCode(), product.getName(), shortages);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 안전재고 수정
     * - 쓰기 권한 검증(소유자 또는 FULL 공유)
     */
    @Transactional
    public InventoryResponse updateSafetyStock(Long userId, Long warehouseId, Long itemId, SafetyStockUpdateRequest request) {
        warehouseShareService.checkFullAccess(userId, warehouseId);
        Inventory inventory = getInventoryOrThrow(warehouseId, itemId);
        inventory.updateSafetyStock(request.safetyStock());
        return InventoryResponse.from(inventory);
    }

    /**
     * 창고 이력 Export용 조회
     * - 조회 권한 검증
     * - 타입·기간 필터 적용
     * - itemCode, itemName 포함한 행 목록 반환
     */
    public List<InventoryExportRow> getWarehouseHistory(
            Long userId, Long warehouseId,
            List<InventoryHistoryType> types,
            LocalDate from, LocalDate to) {
        warehouseShareService.checkViewAccess(userId, warehouseId);
        return inventoryHistoryRepository
                .findAllByInventory_WarehouseIdAndTypeInAndCreatedAtBetween(
                        warehouseId, types,
                        from.atStartOfDay(),
                        to.plusDays(1).atStartOfDay())
                .stream().map(InventoryExportRow::from).toList();
    }

    //======================== 헬퍼 메소드 ===========================//

    /** 창고 + 품목으로 재고 조회, 없으면 예외 */
    private Inventory getInventoryOrThrow(Long warehouseId, Long itemId) {
        return inventoryRepository.findByWarehouseIdAndItemId(warehouseId, itemId)
                .orElseThrow(() -> new ImsException(ErrorCode.INVENTORY_NOT_FOUND));
    }

    /**
     * 부족 분석 대상 완성품을 모은다.
     * 창고 소유자의 완성품과 이 창고에 재고가 있는 완성품의 합집합이다.
     */
    private List<Item> collectAnalysisTargets(Warehouse warehouse, Long warehouseId) {
        Map<Long, Item> targets = new LinkedHashMap<>();

        itemRepository.findAllByOwnerIdAndType(warehouse.getOwner().getId(), ItemType.PRODUCT)
                .forEach(item -> targets.put(item.getId(), item));

        inventoryRepository.findAllByWarehouseId(warehouseId).stream()
                .map(Inventory::getItem)
                .filter(item -> item.getType() == ItemType.PRODUCT)
                .forEach(item -> targets.putIfAbsent(item.getId(), item));

        return List.copyOf(targets.values());
    }

    /**
     * 창고 조회 화면에서 다룰 수 있는 품목인지 판별한다.
     * 창고 접근 권한은 호출 전에 검증되어 있어야 한다.
     *
     * 소유자를 창고 소유자로 고정하면 안 된다. 유통사 창고에 제조사 완성품이
     * 들어가는 것이 정상 케이스라 창고 소유자와 품목 소유자는 자주 다르다.
     */
    private Item resolveWarehouseItem(Long userId, Long warehouseId, Long itemId) {
        return inventoryRepository.findByWarehouseIdAndItemId(warehouseId, itemId)
                .map(Inventory::getItem)
                .orElseGet(() -> domainValidator.getOwnedItem(userId, itemId));
    }
}
