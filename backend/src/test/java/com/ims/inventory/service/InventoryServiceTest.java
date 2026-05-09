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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @InjectMocks
    private InventoryService inventoryService;

    @Mock private InventoryRepository inventoryRepository;
    @Mock private InventoryHistoryRepository inventoryHistoryRepository;
    @Mock private DomainValidator domainValidator;
    @Mock private InventoryHistoryWriter inventoryHistoryWriter;
    @Mock private WarehouseShareService warehouseShareService;
    @Mock private BomService bomService;
    @Mock private ItemRepository itemRepository;

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
        willDoNothing().given(warehouseShareService).checkViewAccess(owner.getId(), warehouse.getId());
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
        given(domainValidator.getOwnedWarehouse(owner.getId(), warehouse.getId())).willReturn(warehouse);
        given(inventoryRepository.findByWarehouseIdAndItemId(warehouse.getId(), itemBike.getId())).willReturn(Optional.of(inventory));

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
        given(domainValidator.getOwnedWarehouse(owner.getId(), warehouse.getId())).willReturn(warehouse);
        given(inventoryRepository.findByWarehouseIdAndItemId(warehouse.getId(), itemBike.getId())).willReturn(Optional.of(inventory));

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
        given(domainValidator.getOwnedWarehouse(owner.getId(), warehouse.getId())).willReturn(warehouse);
        given(inventoryRepository.findByWarehouseIdAndItemId(warehouse.getId(), itemBike.getId())).willReturn(Optional.of(inventory));

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
        given(domainValidator.getOwnedWarehouse(owner.getId(), warehouse.getId())).willReturn(warehouse);
        given(inventoryRepository.findByWarehouseIdAndItemId(warehouse.getId(), itemBike.getId())).willReturn(Optional.of(inventory));

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
        given(domainValidator.getOwnedItem(owner.getId(), itemBike.getId())).willReturn(itemBike);
        given(bomService.getFullBomTree(itemBike.getId(), owner.getId())).willReturn(Map.of());

        MaxProducibleResponse result = inventoryService.calcMaxProducible(owner.getId(), warehouse.getId(), itemBike.getId());

        assertThat(result.maxQuantity()).isNull();
    }

    @Test
    @DisplayName("안전재고 수정 성공")
    void updateSafetyStock_success() {
        SafetyStockUpdateRequest request = new SafetyStockUpdateRequest(30);
        given(domainValidator.getOwnedWarehouse(owner.getId(), warehouse.getId())).willReturn(warehouse);
        given(inventoryRepository.findByWarehouseIdAndItemId(warehouse.getId(), itemBike.getId()))
                .willReturn(Optional.of(inventory));

        InventoryResponse result = inventoryService.updateSafetyStock(owner.getId(), warehouse.getId(), itemBike.getId(), request);

        assertThat(result.safetyStock()).isEqualTo(30);
    }

    @Test
    @DisplayName("안전재고 수정 실패 - 소유자 아님")
    void updateSafetyStock_notOwner() {
        SafetyStockUpdateRequest request = new SafetyStockUpdateRequest(30);
        given(domainValidator.getOwnedWarehouse(999L, warehouse.getId())).willThrow(new ImsException(ErrorCode.WAREHOUSE_NOT_OWNED));

        assertThatThrownBy(() -> inventoryService.updateSafetyStock(999L, warehouse.getId(), itemBike.getId(), request))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WAREHOUSE_NOT_OWNED);
    }

    @Test
    @DisplayName("입출고 이력 조회 성공")
    void getHistory_success() {
        InventoryHistory history = InventoryHistory.builder()
                .id(1L).inventory(inventory).type(InventoryHistoryType.IN).delta(50).memo("입고").build();
        willDoNothing().given(warehouseShareService).checkViewAccess(owner.getId(), warehouse.getId());
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

        willDoNothing().given(warehouseShareService).checkViewAccess(owner.getId(), warehouse.getId());
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
        willDoNothing().given(warehouseShareService).checkViewAccess(owner.getId(), warehouse.getId());
        given(itemRepository.findAllByOwnerIdAndType(owner.getId(), ItemType.PRODUCT)).willReturn(List.of());

        // when
        List<ShortageItemResponse> result = inventoryService.getShortageAnalysis(owner.getId(), warehouse.getId());

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("부족 재고 분석 - BOM 없는 완성품은 분석 결과에서 제외")
    void getShortageAnalysis_noBomProduct_ignored() {
        // given: 자전거에 BOM 없음 → 분석 대상 제외
        willDoNothing().given(warehouseShareService).checkViewAccess(owner.getId(), warehouse.getId());
        given(itemRepository.findAllByOwnerIdAndType(owner.getId(), ItemType.PRODUCT)).willReturn(List.of(itemBike));
        given(bomService.getFullBomTrees(List.of(itemBike.getId()), owner.getId()))
                .willReturn(Map.of(itemBike.getId(), Map.of())); // BOM 없음

        // when
        List<ShortageItemResponse> result = inventoryService.getShortageAnalysis(owner.getId(), warehouse.getId());

        // then: BOM 없는 완성품은 제외
        assertThat(result).isEmpty();
    }
}
