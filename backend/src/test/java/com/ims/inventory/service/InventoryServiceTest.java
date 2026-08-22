package com.ims.inventory.service;

import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.global.support.DomainValidator;
import com.ims.global.support.InventoryHistoryWriter;
import com.ims.inventory.dto.request.AdjustRequest;
import com.ims.inventory.dto.request.InboundRequest;
import com.ims.inventory.dto.request.InventoryCreateRequest;
import com.ims.inventory.dto.request.OutboundRequest;
import com.ims.inventory.dto.request.SafetyStockUpdateRequest;
import com.ims.inventory.dto.response.InventoryResponse;
import com.ims.inventory.dto.response.MaxProducibleResponse;
import com.ims.inventory.dto.response.ShortageItemResponse;
import com.ims.inventory.dto.response.StockDepletionResponse;
import com.ims.inventory.dto.response.StockDepletionRow;
import com.ims.inventory.entity.InventoryHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import com.ims.inventory.entity.Inventory;
import com.ims.inventory.entity.InventoryHistoryType;
import com.ims.inventory.repository.InventoryHistoryRepository;
import com.ims.inventory.repository.InventoryRepository;
import com.ims.item.entity.Item;
import com.ims.item.entity.ItemType;
import com.ims.item.repository.ItemRepository;
import com.ims.item.service.BomService;
import com.ims.user.entity.User;
import com.ims.warehouse.entity.Warehouse;
import com.ims.warehouse.service.WarehouseShareService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @InjectMocks
    private InventoryService inventoryService;

    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private InventoryHistoryRepository inventoryHistoryRepository;
    @Mock
    private DomainValidator domainValidator;
    @Mock
    private InventoryHistoryWriter inventoryHistoryWriter;
    @Mock
    private WarehouseShareService warehouseShareService;
    @Mock
    private BomService bomService;
    @Mock
    private ItemRepository itemRepository;

    private User owner;
    private Warehouse warehouse;
    private Item itemBike;
    private Item itemTire;
    private Item itemFrame;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).email("test@test.com").password("pw").companyName("테스트").companyCode("TC001").build();
        warehouse = Warehouse.builder().id(1L).owner(owner).name("서울창고").build();
        itemBike = Item.builder().id(10L).owner(owner).itemCode("BIKE-001").name("A자전거").type(ItemType.PRODUCT).build();
        itemTire = Item.builder().id(20L).owner(owner).itemCode("TIRE-001").name("타이어").type(ItemType.PART).build();
        itemFrame = Item.builder().id(30L).owner(owner).itemCode("FRAME-001").name("프레임").type(ItemType.PART).build();
        inventory = Inventory.builder().id(100L).warehouse(warehouse).item(itemBike).quantity(50).safetyStock(10).build();
    }

    @Test
    @DisplayName("재고 목록 조회 성공 - 조회 권한 통과 후 결과 반환")
    void getInventories_success() {
        // given: checkViewAccess 통과 (소유자든 공유자든 내부에서 처리)
        given(warehouseShareService.checkViewAccess(owner.getId(), warehouse.getId())).willReturn(warehouse);
        given(inventoryRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .willReturn(new PageImpl<>(List.of(inventory)));

        // when
        Page<InventoryResponse> result = inventoryService.getInventories(
                owner.getId(), warehouse.getId(), null, PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).hasSize(1);
        then(warehouseShareService).should().checkViewAccess(owner.getId(), warehouse.getId());
    }

    @Test
    @DisplayName("재고 목록 조회 실패 - 조회 권한 없음")
    void getInventories_accessDenied() {
        // given: VIEW 권한도 없는 외부 User
        willThrow(new ImsException(ErrorCode.WAREHOUSE_ACCESS_DENIED))
                .given(warehouseShareService).checkViewAccess(999L, warehouse.getId());

        // when & then
        assertThatThrownBy(() -> inventoryService.getInventories(
                999L, warehouse.getId(), null, PageRequest.of(0, 10)))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WAREHOUSE_ACCESS_DENIED);
        then(inventoryRepository).should(never()).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    @DisplayName("재고 항목 등록 성공")
    void createInventory_success() {
        // given
        InventoryCreateRequest request = new InventoryCreateRequest(itemBike.getId(), 10, 5);
        given(domainValidator.getOwnedWarehouse(owner.getId(), warehouse.getId())).willReturn(warehouse);
        given(domainValidator.getOwnedItem(owner.getId(), itemBike.getId())).willReturn(itemBike);
        given(inventoryRepository.existsByWarehouseIdAndItemId(warehouse.getId(), itemBike.getId())).willReturn(false);
        given(inventoryRepository.save(any(Inventory.class))).willReturn(inventory);

        // when
        inventoryService.createInventory(owner.getId(), warehouse.getId(), request);

        // then
        then(inventoryRepository).should().save(any(Inventory.class));
    }

    @Test
    @DisplayName("재고 항목 등록 실패 - 창고 소유자 아님")
    void createInventory_notOwner() {
        // given
        InventoryCreateRequest request = new InventoryCreateRequest(itemBike.getId(), 10, 5);
        given(domainValidator.getOwnedWarehouse(999L, warehouse.getId())).willThrow(new ImsException(ErrorCode.WAREHOUSE_NOT_OWNED));

        // when & then
        assertThatThrownBy(() -> inventoryService.createInventory(999L, warehouse.getId(), request))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WAREHOUSE_NOT_OWNED);
    }

    @Test
    @DisplayName("재고 항목 등록 실패 - 중복 (창고+품목 이미 존재)")
    void createInventory_duplicate() {
        // given
        InventoryCreateRequest request = new InventoryCreateRequest(itemBike.getId(), 10, 5);
        given(domainValidator.getOwnedWarehouse(owner.getId(), warehouse.getId())).willReturn(warehouse);
        given(domainValidator.getOwnedItem(owner.getId(), itemBike.getId())).willReturn(itemBike);
        given(inventoryRepository.existsByWarehouseIdAndItemId(warehouse.getId(), itemBike.getId())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> inventoryService.createInventory(owner.getId(), warehouse.getId(), request))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_INVENTORY);
    }

    @Test
    @DisplayName("입고 성공 - quantity 증가, History(IN, delta=+qty) 기록")
    void adjustIn_success() {
        // given: inventory.quantity=50, 입고 qty=30 → 80
        InboundRequest request = new InboundRequest(30, null);
        given(inventoryRepository.findByWarehouseIdAndItemIdForUpdate(warehouse.getId(), itemBike.getId())).willReturn(Optional.of(inventory));

        // when
        inventoryService.adjustIn(owner.getId(), warehouse.getId(), itemBike.getId(), request);

        // then
        assertThat(inventory.getQuantity()).isEqualTo(80);
        then(inventoryHistoryWriter).should().save(eq(inventory), eq(InventoryHistoryType.IN), eq(30), any());
    }

    @Test
    @DisplayName("출고 성공 - quantity 감소, History(OUT, delta=-qty) 기록")
    void adjustOut_success() {
        // given: inventory.quantity=50, 출고 qty=20 → 30
        OutboundRequest request = new OutboundRequest(20, null);
        given(inventoryRepository.findByWarehouseIdAndItemIdForUpdate(warehouse.getId(), itemBike.getId())).willReturn(Optional.of(inventory));

        // when
        inventoryService.adjustOut(owner.getId(), warehouse.getId(), itemBike.getId(), request);

        // then
        assertThat(inventory.getQuantity()).isEqualTo(30);
        then(inventoryHistoryWriter).should().save(eq(inventory), eq(InventoryHistoryType.OUT), eq(-20), any());
    }

    @Test
    @DisplayName("출고 실패 - 재고 부족 (quantity < qty)")
    void adjustOut_insufficientStock() {
        // given: inventory.quantity=50, 출고 요청 qty=100
        OutboundRequest request = new OutboundRequest(100, null);
        given(inventoryRepository.findByWarehouseIdAndItemIdForUpdate(warehouse.getId(), itemBike.getId())).willReturn(Optional.of(inventory));

        // when & then: Inventory.deduct()에서 INSUFFICIENT_STOCK 발생
        assertThatThrownBy(() -> inventoryService.adjustOut(owner.getId(), warehouse.getId(), itemBike.getId(), request))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_STOCK);
    }

    @Test
    @DisplayName("ADJUSTMENT 성공 - 절대값 보정, delta = newQty - currentQty")
    void adjust_success() {
        // given: inventory.quantity=50, newQty=30 → quantity=30, delta=-20
        AdjustRequest request = new AdjustRequest(30, "재고 실사");
        given(inventoryRepository.findByWarehouseIdAndItemIdForUpdate(warehouse.getId(), itemBike.getId())).willReturn(Optional.of(inventory));

        // when
        inventoryService.adjust(owner.getId(), warehouse.getId(), itemBike.getId(), request);

        // then
        assertThat(inventory.getQuantity()).isEqualTo(30);
        then(inventoryHistoryWriter).should().save(eq(inventory), eq(InventoryHistoryType.ADJUSTMENT), eq(-20), any());
    }

    @Test
    @DisplayName("최대 생산 가능 수량 - min 계산 정상")
    void calcMaxProducible_success() {
        // given: 자전거 BOM = { 타이어: 2, 프레임: 3 }
        //        타이어 재고=100, 프레임 재고=30
        //        max = min(100/2, 30/3) = min(50, 10) = 10
        Inventory tireInventory = Inventory.builder().id(101L).warehouse(warehouse).item(itemTire).quantity(100).safetyStock(0).build();
        Inventory frameInventory = Inventory.builder().id(102L).warehouse(warehouse).item(itemFrame).quantity(30).safetyStock(0).build();

        given(warehouseShareService.checkViewAccess(owner.getId(), warehouse.getId())).willReturn(warehouse);
        given(domainValidator.getOwnedItem(owner.getId(), itemBike.getId())).willReturn(itemBike);
        given(bomService.getFullBomTree(itemBike.getId(), owner.getId())).willReturn(Map.of(itemTire.getId(), 2L, itemFrame.getId(), 3L));
        given(inventoryRepository.findAllByWarehouseIdAndItemIdIn(eq(warehouse.getId()), any()))
                .willReturn(List.of(tireInventory, frameInventory));

        // when
        MaxProducibleResponse result = inventoryService.calcMaxProducible(owner.getId(), warehouse.getId(), itemBike.getId());

        // then
        then(warehouseShareService).should().checkViewAccess(owner.getId(), warehouse.getId());
        assertThat(result.maxQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("최대 생산 가능 수량 - 부품 재고 없으면 0")
    void calcMaxProducible_noStock() {
        // given: 타이어 재고 항목 자체가 DB에 없음
        given(warehouseShareService.checkViewAccess(owner.getId(), warehouse.getId())).willReturn(warehouse);
        given(domainValidator.getOwnedItem(owner.getId(), itemBike.getId())).willReturn(itemBike);
        given(bomService.getFullBomTree(itemBike.getId(), owner.getId())).willReturn(Map.of(itemTire.getId(), 2L));
        given(inventoryRepository.findAllByWarehouseIdAndItemIdIn(eq(warehouse.getId()), any()))
                .willReturn(List.of()); // 재고 항목 없음

        // when
        MaxProducibleResponse result = inventoryService.calcMaxProducible(owner.getId(), warehouse.getId(), itemBike.getId());

        // then
        then(warehouseShareService).should().checkViewAccess(owner.getId(), warehouse.getId());
        assertThat(result.maxQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("최대 생산 가능 수량 - BOM 없으면 null (제약 없음)")
    void calcMaxProducible_noBom() {
        given(warehouseShareService.checkViewAccess(owner.getId(), warehouse.getId())).willReturn(warehouse);
        given(domainValidator.getOwnedItem(owner.getId(), itemBike.getId())).willReturn(itemBike);
        given(bomService.getFullBomTree(itemBike.getId(), owner.getId())).willReturn(Map.of());

        MaxProducibleResponse result = inventoryService.calcMaxProducible(owner.getId(), warehouse.getId(), itemBike.getId());

        assertThat(result.maxQuantity()).isNull();
    }

    @Test
    @DisplayName("안전재고 수정 성공")
    void updateSafetyStock_success() {
        SafetyStockUpdateRequest request = new SafetyStockUpdateRequest(30);
        given(inventoryRepository.findByWarehouseIdAndItemIdForUpdate(warehouse.getId(), itemBike.getId()))
                .willReturn(Optional.of(inventory));

        InventoryResponse result = inventoryService.updateSafetyStock(owner.getId(), warehouse.getId(), itemBike.getId(), request);

        assertThat(result.safetyStock()).isEqualTo(30);
    }

    @Test
    @DisplayName("안전재고 수정 실패 - 쓰기 권한 없음")
    void updateSafetyStock_notOwner() {
        SafetyStockUpdateRequest request = new SafetyStockUpdateRequest(30);
        willThrow(new ImsException(ErrorCode.WAREHOUSE_ACCESS_DENIED))
                .given(warehouseShareService).checkFullAccess(999L, warehouse.getId());

        assertThatThrownBy(() -> inventoryService.updateSafetyStock(999L, warehouse.getId(), itemBike.getId(), request))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WAREHOUSE_ACCESS_DENIED);
    }

    @Test
    @DisplayName("입출고 이력 조회 성공")
    void getHistory_success() {
        InventoryHistory history = InventoryHistory.builder()
                .id(1L).inventory(inventory).type(InventoryHistoryType.IN).delta(50).memo("입고").build();
        given(warehouseShareService.checkViewAccess(owner.getId(), warehouse.getId())).willReturn(warehouse);
        given(inventoryRepository.findByWarehouseIdAndItemId(warehouse.getId(), itemBike.getId()))
                .willReturn(Optional.of(inventory));
        given(inventoryHistoryRepository.findAllByInventoryId(eq(inventory.getId()), any()))
                .willReturn(new PageImpl<>(List.of(history)));

        Page<?> result = inventoryService.getHistory(owner.getId(), warehouse.getId(), itemBike.getId(), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("부족 재고 분석 - 부족 부품 있음: 자전거(타이어 재고 부족)")
    void getShortageAnalysis_hasShortage() {
        // given: 자전거 BOM = { 타이어: 2 }, 타이어 재고 = 1 → 1개 생산도 불가
        Inventory tireInv = Inventory.builder().id(201L).warehouse(warehouse).item(itemTire).quantity(1).safetyStock(0).build();

        given(warehouseShareService.checkViewAccess(owner.getId(), warehouse.getId())).willReturn(warehouse);
        given(itemRepository.findAllByOwnerIdAndType(owner.getId(), ItemType.PRODUCT)).willReturn(List.of(itemBike));
        given(bomService.getFullBomTrees(List.of(itemBike.getId()), owner.getId()))
                .willReturn(Map.of(itemBike.getId(), Map.of(itemTire.getId(), 2L)));
        given(inventoryRepository.findAllByWarehouseIdAndItemIdIn(eq(warehouse.getId()), any()))
                .willReturn(List.of(tireInv));
        given(itemRepository.findAllById(any())).willReturn(List.of(itemTire));

        // when
        List<ShortageItemResponse> result = inventoryService.getShortageAnalysis(owner.getId(), warehouse.getId());

        // then: 자전거 1건, 부족 부품 타이어 1건
        assertThat(result).hasSize(1);
        assertThat(result.get(0).itemId()).isEqualTo(itemBike.getId());
        assertThat(result.get(0).shortages()).hasSize(1);
        assertThat(result.get(0).shortages().get(0).partId()).isEqualTo(itemTire.getId());
    }

    @Test
    @DisplayName("부족 재고 분석 - PRODUCT 없음: 빈 결과 반환")
    void getShortageAnalysis_noProducts() {
        // given: 소유한 PRODUCT 품목 없음
        given(warehouseShareService.checkViewAccess(owner.getId(), warehouse.getId())).willReturn(warehouse);
        given(itemRepository.findAllByOwnerIdAndType(owner.getId(), ItemType.PRODUCT)).willReturn(List.of());

        // when
        List<ShortageItemResponse> result = inventoryService.getShortageAnalysis(owner.getId(), warehouse.getId());

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("부족 재고 분석 - 완성품이 여러 개여도 재고·부품 조회는 각각 1회")
    void getShortageAnalysis_batchesQueriesRegardlessOfProductCount() {
        // given: 완성품 2개가 각각 다른 부품을 쓴다
        Item itemCar = Item.builder().id(11L).owner(owner).itemCode("CAR-001")
                .name("B자동차").type(ItemType.PRODUCT).build();
        Inventory tireInv = Inventory.builder().id(201L).warehouse(warehouse)
                .item(itemTire).quantity(1).safetyStock(0).build();
        Inventory frameInv = Inventory.builder().id(202L).warehouse(warehouse)
                .item(itemFrame).quantity(0).safetyStock(0).build();

        given(warehouseShareService.checkViewAccess(owner.getId(), warehouse.getId())).willReturn(warehouse);
        given(itemRepository.findAllByOwnerIdAndType(owner.getId(), ItemType.PRODUCT))
                .willReturn(List.of(itemBike, itemCar));
        given(bomService.getFullBomTrees(List.of(itemBike.getId(), itemCar.getId()), owner.getId()))
                .willReturn(Map.of(
                        itemBike.getId(), Map.of(itemTire.getId(), 2L),
                        itemCar.getId(), Map.of(itemFrame.getId(), 3L)));
        given(inventoryRepository.findAllByWarehouseIdAndItemIdIn(eq(warehouse.getId()), any()))
                .willReturn(List.of(tireInv, frameInv));
        given(itemRepository.findAllById(any())).willReturn(List.of(itemTire, itemFrame));

        // when
        List<ShortageItemResponse> result =
                inventoryService.getShortageAnalysis(owner.getId(), warehouse.getId());

        // then: 두 완성품 모두 부족 판정
        assertThat(result).hasSize(2);

        // 완성품 수와 무관하게 조회는 각각 1회여야 한다.
        // 루프 안에서 조회하면 완성품 수만큼 호출되어 이 단언이 깨진다.
        then(inventoryRepository).should(times(1))
                .findAllByWarehouseIdAndItemIdIn(eq(warehouse.getId()), any());
        then(itemRepository).should(times(1)).findAllById(any());
    }

    @Test
    @DisplayName("부족 재고 분석 - 부족 부품이 없으면 부품 정보를 조회하지 않는다")
    void getShortageAnalysis_noShortage_skipsItemLookup() {
        // given: 타이어 재고가 충분하다
        Inventory tireInv = Inventory.builder().id(201L).warehouse(warehouse)
                .item(itemTire).quantity(100).safetyStock(0).build();

        given(warehouseShareService.checkViewAccess(owner.getId(), warehouse.getId())).willReturn(warehouse);
        given(itemRepository.findAllByOwnerIdAndType(owner.getId(), ItemType.PRODUCT)).willReturn(List.of(itemBike));
        given(bomService.getFullBomTrees(List.of(itemBike.getId()), owner.getId()))
                .willReturn(Map.of(itemBike.getId(), Map.of(itemTire.getId(), 2L)));
        given(inventoryRepository.findAllByWarehouseIdAndItemIdIn(eq(warehouse.getId()), any()))
                .willReturn(List.of(tireInv));

        // when
        List<ShortageItemResponse> result =
                inventoryService.getShortageAnalysis(owner.getId(), warehouse.getId());

        // then
        assertThat(result).isEmpty();
        then(itemRepository).should(never()).findAllById(any());
    }

    @Test
    @DisplayName("부족 재고 분석 - BOM 없는 완성품은 분석 결과에서 제외")
    void getShortageAnalysis_noBomProduct_ignored() {
        // given: 자전거에 BOM 없음 → 분석 대상 제외
        given(warehouseShareService.checkViewAccess(owner.getId(), warehouse.getId())).willReturn(warehouse);
        given(itemRepository.findAllByOwnerIdAndType(owner.getId(), ItemType.PRODUCT)).willReturn(List.of(itemBike));
        given(bomService.getFullBomTrees(List.of(itemBike.getId()), owner.getId()))
                .willReturn(Map.of(itemBike.getId(), Map.of())); // BOM 없음

        // when
        List<ShortageItemResponse> result = inventoryService.getShortageAnalysis(owner.getId(), warehouse.getId());

        // then: BOM 없는 완성품은 제외
        assertThat(result).isEmpty();
    }

    // ===================== 공유 사용자 =====================
    // 품목·BOM은 창고 소유자 소유다. 호출자 기준으로 조회하면 공유받은 사용자는
    // 소유자의 품목을 찾지 못한다.

    @Test
    @DisplayName("최대 생산 가능 수량 - 공유받은 사용자는 창고 재고에서 품목을 해석한다")
    void calcMaxProducible_sharedUser_resolvesItemFromInventory() {
        // given: 공유받은 사용자(999L)가 owner의 창고를 조회한다
        Long sharedUserId = 999L;
        Inventory tireInventory = Inventory.builder().id(101L).warehouse(warehouse)
                .item(itemTire).quantity(100).safetyStock(0).build();

        Inventory bikeInventory = Inventory.builder().id(102L).warehouse(warehouse)
                .item(itemBike).quantity(0).safetyStock(0).build();

        given(warehouseShareService.checkViewAccess(sharedUserId, warehouse.getId())).willReturn(warehouse);
        given(inventoryRepository.findByWarehouseIdAndItemId(warehouse.getId(), itemBike.getId()))
                .willReturn(Optional.of(bikeInventory));
        given(bomService.getFullBomTree(itemBike.getId(), owner.getId()))
                .willReturn(Map.of(itemTire.getId(), 2L));
        given(inventoryRepository.findAllByWarehouseIdAndItemIdIn(eq(warehouse.getId()), any()))
                .willReturn(List.of(tireInventory));

        // when
        MaxProducibleResponse result =
                inventoryService.calcMaxProducible(sharedUserId, warehouse.getId(), itemBike.getId());

        // then: 호출자(999L) 소유 품목을 찾으면 안 된다. 창고 재고에서 품목을 찾고
        //       BOM은 그 품목의 소유자(1L) 기준으로 조회해야 한다
        then(domainValidator).should(never()).getOwnedItem(eq(sharedUserId), any());
        then(bomService).should().getFullBomTree(itemBike.getId(), owner.getId());
        assertThat(result.maxQuantity()).isEqualTo(50);
    }

    @Test
    @DisplayName("부족 재고 분석 - 공유받은 사용자도 창고 소유자의 완성품을 분석 대상에 넣는다")
    void getShortageAnalysis_sharedUser_includesWarehouseOwnerProducts() {
        // given
        Long sharedUserId = 999L;
        Inventory tireInv = Inventory.builder().id(201L).warehouse(warehouse)
                .item(itemTire).quantity(1).safetyStock(0).build();

        given(warehouseShareService.checkViewAccess(sharedUserId, warehouse.getId())).willReturn(warehouse);
        given(itemRepository.findAllByOwnerIdAndType(owner.getId(), ItemType.PRODUCT))
                .willReturn(List.of(itemBike));
        given(bomService.getFullBomTrees(List.of(itemBike.getId()), owner.getId()))
                .willReturn(Map.of(itemBike.getId(), Map.of(itemTire.getId(), 2L)));
        given(inventoryRepository.findAllByWarehouseIdAndItemIdIn(eq(warehouse.getId()), any()))
                .willReturn(List.of(tireInv));
        given(itemRepository.findAllById(any())).willReturn(List.of(itemTire));

        // when
        List<ShortageItemResponse> result =
                inventoryService.getShortageAnalysis(sharedUserId, warehouse.getId());

        // then: 호출자 기준으로 조회하면 남의 창고를 보면서 자기 완성품을 분석하게 된다
        then(itemRepository).should().findAllByOwnerIdAndType(owner.getId(), ItemType.PRODUCT);
        then(itemRepository).should(never()).findAllByOwnerIdAndType(eq(sharedUserId), any());
        assertThat(result).hasSize(1);
        assertThat(result.get(0).itemId()).isEqualTo(itemBike.getId());
    }

    // ===================== 쓰기 권한 =====================
    // 재고 변경은 소유자 전용이 아니라 FULL 권한 공유자도 가능해야 한다.
    // 소유자 전용 검증(getOwnedWarehouse)을 쓰면 FULL 권한이 VIEW와 같아진다.

    @Test
    @DisplayName("입고 - 소유자 전용이 아니라 FULL 권한 검증을 사용한다")
    void adjustIn_usesFullAccessCheck() {
        // given
        InboundRequest request = new InboundRequest(30, null);
        given(inventoryRepository.findByWarehouseIdAndItemIdForUpdate(warehouse.getId(), itemBike.getId()))
                .willReturn(Optional.of(inventory));

        // when
        inventoryService.adjustIn(owner.getId(), warehouse.getId(), itemBike.getId(), request);

        // then
        then(warehouseShareService).should().checkFullAccess(owner.getId(), warehouse.getId());
        then(domainValidator).should(never()).getOwnedWarehouse(any(), any());
    }

    @Test
    @DisplayName("출고 - 소유자 전용이 아니라 FULL 권한 검증을 사용한다")
    void adjustOut_usesFullAccessCheck() {
        OutboundRequest request = new OutboundRequest(10, null);
        given(inventoryRepository.findByWarehouseIdAndItemIdForUpdate(warehouse.getId(), itemBike.getId()))
                .willReturn(Optional.of(inventory));

        inventoryService.adjustOut(owner.getId(), warehouse.getId(), itemBike.getId(), request);

        then(warehouseShareService).should().checkFullAccess(owner.getId(), warehouse.getId());
        then(domainValidator).should(never()).getOwnedWarehouse(any(), any());
    }

    @Test
    @DisplayName("재고 보정 - 소유자 전용이 아니라 FULL 권한 검증을 사용한다")
    void adjust_usesFullAccessCheck() {
        AdjustRequest request = new AdjustRequest(80, null);
        given(inventoryRepository.findByWarehouseIdAndItemIdForUpdate(warehouse.getId(), itemBike.getId()))
                .willReturn(Optional.of(inventory));

        inventoryService.adjust(owner.getId(), warehouse.getId(), itemBike.getId(), request);

        then(warehouseShareService).should().checkFullAccess(owner.getId(), warehouse.getId());
        then(domainValidator).should(never()).getOwnedWarehouse(any(), any());
    }

    @Test
    @DisplayName("안전재고 수정 - 소유자 전용이 아니라 FULL 권한 검증을 사용한다")
    void updateSafetyStock_usesFullAccessCheck() {
        given(inventoryRepository.findByWarehouseIdAndItemIdForUpdate(warehouse.getId(), itemBike.getId()))
                .willReturn(Optional.of(inventory));

        inventoryService.updateSafetyStock(
                owner.getId(), warehouse.getId(), itemBike.getId(), new SafetyStockUpdateRequest(20));

        then(warehouseShareService).should().checkFullAccess(owner.getId(), warehouse.getId());
        then(domainValidator).should(never()).getOwnedWarehouse(any(), any());
    }

    @Test
    @DisplayName("입고 실패 - VIEW 권한만 있으면 거부된다")
    void adjustIn_viewOnlyUser_denied() {
        // given: checkFullAccess가 VIEW 사용자를 거부한다
        Long viewUserId = 888L;
        willThrow(new ImsException(ErrorCode.WAREHOUSE_ACCESS_DENIED))
                .given(warehouseShareService).checkFullAccess(viewUserId, warehouse.getId());

        // when & then
        assertThatThrownBy(() -> inventoryService.adjustIn(
                viewUserId, warehouse.getId(), itemBike.getId(), new InboundRequest(30, null)))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WAREHOUSE_ACCESS_DENIED);

        // 권한 검증에서 막혀 재고를 건드리지 않아야 한다
        then(inventoryRepository).should(never()).findByWarehouseIdAndItemIdForUpdate(any(), any());
        then(inventoryHistoryWriter).shouldHaveNoInteractions();
    }

    // ===================== 창고 소유자 ≠ 품목 소유자 =====================
    // 유통사 창고에 제조사 완성품이 들어가는 것은 정상 케이스다.
    // 창고 소유자 기준으로만 품목을 찾으면 이 창고의 재고를 아예 다룰 수 없다.

    @Test
    @DisplayName("최대 생산량 - 창고 소유자와 품목 소유자가 달라도 창고 재고의 품목이면 계산한다")
    void calcMaxProducible_itemOwnedByOtherUser_resolvedFromInventory() {
        // given: 창고는 999L 소유, 그 안의 완성품은 owner(1L) 소유
        User warehouseOwner = User.builder().id(999L).email("dist@test.com").password("pw")
                .companyName("유통사").companyCode("DS001").build();
        Warehouse distWarehouse = Warehouse.builder().id(77L).owner(warehouseOwner).name("유통창고").build();

        Inventory bikeInv = Inventory.builder().id(301L).warehouse(distWarehouse)
                .item(itemBike).quantity(5).safetyStock(0).build();
        Inventory tireInv = Inventory.builder().id(302L).warehouse(distWarehouse)
                .item(itemTire).quantity(100).safetyStock(0).build();

        given(warehouseShareService.checkViewAccess(owner.getId(), distWarehouse.getId()))
                .willReturn(distWarehouse);
        given(inventoryRepository.findByWarehouseIdAndItemId(distWarehouse.getId(), itemBike.getId()))
                .willReturn(Optional.of(bikeInv));
        given(bomService.getFullBomTree(itemBike.getId(), owner.getId()))
                .willReturn(Map.of(itemTire.getId(), 2L));
        given(inventoryRepository.findAllByWarehouseIdAndItemIdIn(eq(distWarehouse.getId()), any()))
                .willReturn(List.of(tireInv));

        // when
        MaxProducibleResponse result =
                inventoryService.calcMaxProducible(owner.getId(), distWarehouse.getId(), itemBike.getId());

        // then: BOM은 창고 소유자(999L)가 아니라 품목 소유자(1L) 기준으로 조회해야 한다
        then(bomService).should().getFullBomTree(itemBike.getId(), owner.getId());
        then(bomService).should(never()).getFullBomTree(any(), eq(warehouseOwner.getId()));
        assertThat(result.maxQuantity()).isEqualTo(50);
    }

    @Test
    @DisplayName("부족 재고 분석 - 창고 소유자 품목이 없어도 창고에 있는 완성품을 분석한다")
    void getShortageAnalysis_productsStockedByOtherUser_areAnalyzed() {
        // given: 창고 소유자(999L)는 완성품이 없고, 창고에는 owner(1L)의 완성품이 들어 있다
        User warehouseOwner = User.builder().id(999L).email("dist@test.com").password("pw")
                .companyName("유통사").companyCode("DS001").build();
        Warehouse distWarehouse = Warehouse.builder().id(77L).owner(warehouseOwner).name("유통창고").build();

        Inventory bikeInv = Inventory.builder().id(301L).warehouse(distWarehouse)
                .item(itemBike).quantity(5).safetyStock(0).build();
        Inventory tireInv = Inventory.builder().id(302L).warehouse(distWarehouse)
                .item(itemTire).quantity(1).safetyStock(0).build();

        given(warehouseShareService.checkViewAccess(owner.getId(), distWarehouse.getId()))
                .willReturn(distWarehouse);
        given(itemRepository.findAllByOwnerIdAndType(warehouseOwner.getId(), ItemType.PRODUCT))
                .willReturn(List.of());
        given(inventoryRepository.findAllByWarehouseId(distWarehouse.getId()))
                .willReturn(List.of(bikeInv, tireInv));
        given(bomService.getFullBomTrees(List.of(itemBike.getId()), owner.getId()))
                .willReturn(Map.of(itemBike.getId(), Map.of(itemTire.getId(), 2L)));
        given(inventoryRepository.findAllByWarehouseIdAndItemIdIn(eq(distWarehouse.getId()), any()))
                .willReturn(List.of(tireInv));
        given(itemRepository.findAllById(any())).willReturn(List.of(itemTire));

        // when
        List<ShortageItemResponse> result =
                inventoryService.getShortageAnalysis(owner.getId(), distWarehouse.getId());

        // then: 창고 소유자에게 완성품이 없어도 조용히 0건이 되면 안 된다
        assertThat(result).hasSize(1);
        assertThat(result.get(0).itemId()).isEqualTo(itemBike.getId());
    }

    // ===================== 재고 소진 예측 =====================
    // 기간 내 나간 양으로 월평균을 내고 현재 재고가 몇 달치인지 계산한다.
    // 소진량은 OUT + PRODUCTION_DEDUCTION 이다. 조립 창고는 후자가 주 경로다.

    @Test
    @DisplayName("소진 예측 - 월평균 출고량과 잔여 개월을 계산한다")
    void getDepletionAnalysis_calculatesMonthlyAverage() {
        // given: 90일(3개월) 동안 OUT 100 + OUT 50 + 생산차감 30 = 180 소진, 현재 재고 300
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = from.plusDays(89); // 90일 → months = 3.0

        Inventory bikeInventory = Inventory.builder().id(100L).warehouse(warehouse)
                .item(itemBike).quantity(300).safetyStock(50).build();

        given(warehouseShareService.checkViewAccess(owner.getId(), warehouse.getId())).willReturn(warehouse);
        given(inventoryRepository.findAllByWarehouseId(warehouse.getId()))
                .willReturn(List.of(bikeInventory));
        given(inventoryHistoryRepository.findAllByInventory_WarehouseIdAndTypeInAndCreatedAtBetween(
                eq(warehouse.getId()),
                eq(List.of(InventoryHistoryType.OUT, InventoryHistoryType.PRODUCTION_DEDUCTION)),
                any(), any()))
                .willReturn(List.of(
                        history(bikeInventory, InventoryHistoryType.OUT, -100),
                        history(bikeInventory, InventoryHistoryType.OUT, -50),
                        history(bikeInventory, InventoryHistoryType.PRODUCTION_DEDUCTION, -30)));

        // when
        StockDepletionResponse result =
                inventoryService.getDepletionAnalysis(owner.getId(), warehouse.getId(), from, to);

        // then: delta가 음수라 Math.abs로 합산해야 180이 나온다
        assertThat(result.months()).isEqualTo(3.0);
        assertThat(result.rows()).hasSize(1);

        StockDepletionRow row = result.rows().get(0);
        assertThat(row.totalOutbound()).isEqualTo(180);
        assertThat(row.monthlyAverage()).isEqualTo(60.0);   // 180 / 3
        assertThat(row.monthsRemaining()).isEqualTo(5.0);   // 300 / 60
    }

    @Test
    @DisplayName("소진 예측 - 기간 내 출고가 없으면 잔여 개월은 null이다")
    void getDepletionAnalysis_noOutbound_monthsRemainingIsNull() {
        // given: 재고는 있지만 기간 내 나간 이력이 없다
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = from.plusDays(89);

        Inventory bikeInventory = Inventory.builder().id(100L).warehouse(warehouse)
                .item(itemBike).quantity(300).safetyStock(50).build();

        given(warehouseShareService.checkViewAccess(owner.getId(), warehouse.getId())).willReturn(warehouse);
        given(inventoryRepository.findAllByWarehouseId(warehouse.getId()))
                .willReturn(List.of(bikeInventory));
        given(inventoryHistoryRepository.findAllByInventory_WarehouseIdAndTypeInAndCreatedAtBetween(
                eq(warehouse.getId()), any(), any(), any()))
                .willReturn(List.of());

        // when
        StockDepletionResponse result =
                inventoryService.getDepletionAnalysis(owner.getId(), warehouse.getId(), from, to);

        // then: 줄지 않는 재고는 소진 시점을 말할 수 없다. 0.0이면 "곧 소진"으로 읽힌다
        StockDepletionRow row = result.rows().get(0);
        assertThat(row.totalOutbound()).isZero();
        assertThat(row.monthlyAverage()).isEqualTo(0.0);
        assertThat(row.monthsRemaining()).isNull();


    }

    @Test
    @DisplayName("소진 예측 - 기간 내 출고가 없던 품목도 결과에 포함한다")
    void getDepletionAnalysis_includesItemsWithoutHistory() {
        //  given  재고 2건(itemBike, itemTire) / 이력은 itemBike 것만
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = from.plusDays(89);

        Inventory bikeInv = Inventory.builder().id(100L).warehouse(warehouse)
                .item(itemBike).quantity(300).safetyStock(50).build();
        Inventory tireInv = Inventory.builder().id(200L).warehouse(warehouse)
                .item(itemTire).quantity(300).safetyStock(50).build();

        given(inventoryRepository.findAllByWarehouseId(warehouse.getId()))
                .willReturn(List.of(bikeInv, tireInv));
        given(inventoryHistoryRepository.findAllByInventory_WarehouseIdAndTypeInAndCreatedAtBetween(
                eq(warehouse.getId()), any(), any(), any()
        )).willReturn(List.of(history(bikeInv, InventoryHistoryType.OUT, -100)));

        given(warehouseShareService.checkViewAccess(owner.getId(), warehouse.getId())).willReturn(warehouse);
        
        // when
        StockDepletionResponse result =
                inventoryService.getDepletionAnalysis(owner.getId(), warehouse.getId(), from, to);

        //  then   rows 2건. itemTire 는 totalOutbound 0, monthsRemaining null
        assertThat(result.rows()).hasSize(2);

        StockDepletionRow tire = result.rows().get(1);

        assertThat(tire.itemId()).isEqualTo(itemTire.getId());
        assertThat(tire.totalOutbound()).isZero();
        assertThat(tire.monthsRemaining()).isNull();
    }

    @Test
    @DisplayName("소진 예측 - 조회 권한을 검증한다")
    void getDepletionAnalysis_checksViewAccess() {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = from.plusDays(89);


        willThrow(new ImsException(ErrorCode.WAREHOUSE_ACCESS_DENIED))
                .given(warehouseShareService).checkViewAccess(999L, warehouse.getId());

        assertThatThrownBy(() -> inventoryService.getDepletionAnalysis(
                999L, warehouse.getId(), from, to))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WAREHOUSE_ACCESS_DENIED);

// 권한 실패 시 DB를 건드리면 안 된다
        then(inventoryRepository).should(never()).findAllByWarehouseId(any());
    }

    /** 소진 예측 테스트용 이력 생성 (createdAt은 서비스가 조회 조건으로만 쓰므로 생략) */
    private InventoryHistory history(Inventory inventory, InventoryHistoryType type, int delta) {
        return InventoryHistory.builder()
                .inventory(inventory)
                .type(type)
                .delta(delta)
                .build();
    }
}
