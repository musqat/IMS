package com.ims.inventory.service;

import com.ims.global.exception.ImsException;
import com.ims.inventory.dto.request.InventoryCreateRequest;
import com.ims.inventory.dto.request.AdjustRequest;
import com.ims.inventory.dto.request.InboundRequest;
import com.ims.inventory.dto.request.OutboundRequest;
import com.ims.inventory.dto.response.MaxProducibleResponse;
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
import com.ims.warehouse.repository.WarehouseRepository;
import com.ims.warehouse.service.WarehouseShareService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private WarehouseRepository warehouseRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private WarehouseShareService warehouseShareService;
    @Mock
    private BomService bomService;

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
        itemBike = Item.builder().id(10L).owner(owner).itemCode("BIKE-001").name("A자전거").type(ItemType.FINISHED).build();
        itemTire = Item.builder().id(20L).owner(owner).itemCode("TIRE-001").name("타이어").type(ItemType.PART).build();
        itemFrame = Item.builder().id(30L).owner(owner).itemCode("FRAME-001").name("프레임").type(ItemType.PART).build();
        inventory = Inventory.builder().id(100L).warehouse(warehouse).item(itemBike).quantity(50).safetyStock(10).build();
    }

    @Test
    @DisplayName("재고 항목 등록 성공")
    void createInventory_success() {
        // given
        InventoryCreateRequest request = new InventoryCreateRequest(itemBike.getId(), 10);
        given(warehouseRepository.findById(warehouse.getId())).willReturn(Optional.of(warehouse)); // getOwnedWarehouse 내부
        given(itemRepository.findById(itemBike.getId())).willReturn(Optional.of(itemBike));
        given(inventoryRepository.existsByWarehouseIdAndItemId(warehouse.getId(), itemBike.getId())).willReturn(false);
        given(inventoryRepository.save(any(Inventory.class))).willReturn(inventory);

        // when
        inventoryService.createInventory(owner.getId(), warehouse.getId(), request);

        // then
        then(inventoryRepository).should().save(any(Inventory.class));
    }

    @Test
    @DisplayName("재고 항목 등록 실패 - 소유자 아님")
    void createInventory_notOwner() {
        // given
        InventoryCreateRequest request = new InventoryCreateRequest(itemBike.getId(), 10);
        given(warehouseRepository.findById(warehouse.getId())).willReturn(Optional.of(warehouse)); // getOwnedWarehouse 내부

        // when & then: userId=999L (소유자 아님)
        assertThatThrownBy(() -> inventoryService.createInventory(999L, warehouse.getId(), request))
                .isInstanceOf(ImsException.class);
    }

    @Test
    @DisplayName("재고 항목 등록 실패 - 중복 (창고+품목 이미 존재)")
    void createInventory_duplicate() {
        // given
        InventoryCreateRequest request = new InventoryCreateRequest(itemBike.getId(), 10);
        given(warehouseRepository.findById(warehouse.getId())).willReturn(Optional.of(warehouse)); // getOwnedWarehouse 내부
        given(itemRepository.findById(itemBike.getId())).willReturn(Optional.of(itemBike));
        given(inventoryRepository.existsByWarehouseIdAndItemId(warehouse.getId(), itemBike.getId())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> inventoryService.createInventory(owner.getId(), warehouse.getId(), request))
                .isInstanceOf(ImsException.class);
    }

    @Test
    @DisplayName("입고 성공 - quantity 증가, History(IN, delta=+qty) 기록")
    void adjustIn_success() {
        // given
        InboundRequest request = new InboundRequest(30, null);
        given(warehouseRepository.findById(warehouse.getId())).willReturn(Optional.of(warehouse)); // getOwnedWarehouse 내부
        given(inventoryRepository.findByWarehouseIdAndItemId(warehouse.getId(), itemBike.getId())).willReturn(Optional.of(inventory));

        // when
        inventoryService.adjustIn(owner.getId(), warehouse.getId(), itemBike.getId(), request);

        // then
        then(inventoryHistoryRepository).should().save(argThat(h ->
                h.getType() == InventoryHistoryType.IN && h.getDelta() == 30));
    }

    @Test
    @DisplayName("출고 성공 - quantity 감소, History(OUT, delta=-qty) 기록")
    void adjustOut_success() {
        // given
        OutboundRequest request = new OutboundRequest(20, null);
        given(warehouseRepository.findById(warehouse.getId())).willReturn(Optional.of(warehouse)); // getOwnedWarehouse 내부
        given(inventoryRepository.findByWarehouseIdAndItemId(warehouse.getId(), itemBike.getId())).willReturn(Optional.of(inventory));

        // when
        inventoryService.adjustOut(owner.getId(), warehouse.getId(), itemBike.getId(), request);

        // then
        then(inventoryHistoryRepository).should().save(argThat(h ->
                h.getType() == InventoryHistoryType.OUT && h.getDelta() == -20));
    }

    @Test
    @DisplayName("출고 실패 - 재고 부족 (quantity < qty)")
    void adjustOut_insufficientStock() {
        // given: inventory.quantity=50, 출고 요청 qty=100
        OutboundRequest request = new OutboundRequest(100, null);
        given(warehouseRepository.findById(warehouse.getId())).willReturn(Optional.of(warehouse)); // getOwnedWarehouse 내부
        given(inventoryRepository.findByWarehouseIdAndItemId(warehouse.getId(), itemBike.getId())).willReturn(Optional.of(inventory));

        // when & then: Inventory.deduct()에서 INSUFFICIENT_STOCK 발생
        assertThatThrownBy(() -> inventoryService.adjustOut(owner.getId(), warehouse.getId(), itemBike.getId(), request))
                .isInstanceOf(ImsException.class);
    }

    @Test
    @DisplayName("ADJUSTMENT 성공 - 절대값 보정, delta = newQty - currentQty")
    void adjust_success() {
        // given: inventory.quantity=50, newQty=30, delta=-20
        AdjustRequest request = new AdjustRequest(30, "재고 실사");
        given(warehouseRepository.findById(warehouse.getId())).willReturn(Optional.of(warehouse)); // getOwnedWarehouse 내부
        given(inventoryRepository.findByWarehouseIdAndItemId(warehouse.getId(), itemBike.getId())).willReturn(Optional.of(inventory));

        // when
        inventoryService.adjust(owner.getId(), warehouse.getId(), itemBike.getId(), request);

        // then
        then(inventoryHistoryRepository).should().save(argThat(h ->
                h.getType() == InventoryHistoryType.ADJUSTMENT && h.getDelta() == -20));
    }

    @Test
    @DisplayName("최대 생산 가능 수량 - min 계산 정상")
    void calcMaxProducible_success() {
        // given: 자전거 BOM = { 타이어: 2, 프레임: 3 }
        //        타이어 재고=100, 프레임 재고=30
        //        max = min(100/2, 30/3) = min(50, 10) = 10
        Inventory tireInventory = Inventory.builder().id(101L).warehouse(warehouse).item(itemTire).quantity(100).safetyStock(0).build();
        Inventory frameInventory = Inventory.builder().id(102L).warehouse(warehouse).item(itemFrame).quantity(30).safetyStock(0).build();

        given(itemRepository.findById(itemBike.getId())).willReturn(Optional.of(itemBike));
        given(bomService.getFullBomTree(itemBike.getId())).willReturn(Map.of(itemTire.getId(), 2, itemFrame.getId(), 3));
        given(inventoryRepository.findByWarehouseIdAndItemId(warehouse.getId(), itemTire.getId())).willReturn(Optional.of(tireInventory));
        given(inventoryRepository.findByWarehouseIdAndItemId(warehouse.getId(), itemFrame.getId())).willReturn(Optional.of(frameInventory));

        // when
        MaxProducibleResponse result = inventoryService.calcMaxProducible(owner.getId(), warehouse.getId(), itemBike.getId());

        // then
        then(warehouseShareService).should().checkViewAccess(owner.getId(), warehouse.getId());
        assertThat(result.maxQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("최대 생산 가능 수량 - 부품 재고 없으면 0")
    void calcMaxProducible_noStock() {
        // given: 타이어 재고 없음
        given(itemRepository.findById(itemBike.getId())).willReturn(Optional.of(itemBike));
        given(bomService.getFullBomTree(itemBike.getId())).willReturn(Map.of(itemTire.getId(), 2));
        given(inventoryRepository.findByWarehouseIdAndItemId(warehouse.getId(), itemTire.getId())).willReturn(Optional.empty());

        // when
        MaxProducibleResponse result = inventoryService.calcMaxProducible(owner.getId(), warehouse.getId(), itemBike.getId());

        // then
        then(warehouseShareService).should().checkViewAccess(owner.getId(), warehouse.getId());
        assertThat(result.maxQuantity()).isEqualTo(0);
    }
}
