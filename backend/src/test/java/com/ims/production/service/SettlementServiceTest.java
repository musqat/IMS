package com.ims.production.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ims.inventory.entity.Inventory;
import com.ims.inventory.repository.InventoryHistoryRepository;
import com.ims.inventory.repository.InventoryRepository;
import com.ims.item.entity.Item;
import com.ims.item.entity.ItemType;
import com.ims.item.service.BomService;
import com.ims.production.entity.*;
import com.ims.production.repository.SettlementRepository;
import com.ims.user.entity.User;
import com.ims.warehouse.entity.Warehouse;
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
class SettlementServiceTest {

    @InjectMocks
    private SettlementService settlementService;

    @Mock private InventoryRepository inventoryRepository;
    @Mock private InventoryHistoryRepository inventoryHistoryRepository;
    @Mock private SettlementRepository settlementRepository;
    @Mock private BomService bomService;
    @Mock private ObjectMapper objectMapper;

    private User owner;
    private Warehouse warehouse;
    private Item itemBike;
    private Item itemTire;
    private Item itemFrame;
    private ProductionRecord record;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).email("test@test.com").password("pw").companyName("테스트").companyCode("TC001").build();
        warehouse = Warehouse.builder().id(1L).owner(owner).name("서울창고").build();
        itemBike = Item.builder().id(10L).owner(owner).itemCode("BIKE-001").name("A자전거").type(ItemType.PRODUCT).build();
        itemTire = Item.builder().id(20L).owner(owner).itemCode("TIRE-001").name("타이어").type(ItemType.PART).build();
        itemFrame = Item.builder().id(30L).owner(owner).itemCode("FRAME-001").name("프레임").type(ItemType.PART).build();
        record = ProductionRecord.builder()
                .id(1L).warehouse(warehouse).item(itemBike).quantity(5)
                .status(ProductionStatus.PENDING).build();
    }

    @Test
    @DisplayName("결산 성공 - 부품 재고 충분, SUCCESS")
    void settle_success() {
        // given: 자전거 BOM = { 타이어: 2, 프레임: 1 }, 생산 5개
        // 필요: 타이어 10개, 프레임 5개 / 재고: 타이어 100개, 프레임 50개
        Inventory tireInv = Inventory.builder().id(100L).warehouse(warehouse).item(itemTire).quantity(100).safetyStock(0).build();
        Inventory frameInv = Inventory.builder().id(101L).warehouse(warehouse).item(itemFrame).quantity(50).safetyStock(0).build();

        given(bomService.getFullBomTree(itemBike.getId())).willReturn(Map.of(itemTire.getId(), 2, itemFrame.getId(), 1));
        given(inventoryRepository.findByWarehouseIdAndItemId(warehouse.getId(), itemTire.getId())).willReturn(Optional.of(tireInv));
        given(inventoryRepository.findByWarehouseIdAndItemId(warehouse.getId(), itemFrame.getId())).willReturn(Optional.of(frameInv));
        given(settlementRepository.save(any(Settlement.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        Settlement result = settlementService.settle(record);

        // then
        assertThat(result.getResult()).isEqualTo(SettlementResult.SUCCESS);
        assertThat(result.getAnomalyDetail()).isNull();
        then(inventoryHistoryRepository).should(times(2)).save(any());
    }

    @Test
    @DisplayName("결산 이상 - 부품 재고 부족, ANOMALY")
    void settle_anomaly() throws Exception {
        // given: 자전거 BOM = { 타이어: 2 }, 생산 5개
        // 필요: 타이어 10개, 재고: 3개 → ANOMALY
        Inventory tireInv = Inventory.builder().id(100L).warehouse(warehouse).item(itemTire).quantity(3).safetyStock(0).build();

        given(bomService.getFullBomTree(itemBike.getId())).willReturn(Map.of(itemTire.getId(), 2));
        given(inventoryRepository.findByWarehouseIdAndItemId(warehouse.getId(), itemTire.getId())).willReturn(Optional.of(tireInv));
        given(objectMapper.writeValueAsString(any())).willReturn("{\"20\":{\"required\":10,\"stock\":3}}");
        given(settlementRepository.save(any(Settlement.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        Settlement result = settlementService.settle(record);

        // then
        assertThat(result.getResult()).isEqualTo(SettlementResult.ANOMALY);
        assertThat(result.getAnomalyDetail()).isNotNull();
    }
}
