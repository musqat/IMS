package com.ims.production.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ims.global.support.InventoryHistoryWriter;
import com.ims.inventory.entity.Inventory;
import com.ims.inventory.repository.InventoryRepository;
import com.ims.item.entity.Item;
import com.ims.item.entity.ItemType;
import com.ims.item.repository.ItemRepository;
import com.ims.item.service.BomService;
import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.production.entity.*;
import com.ims.production.repository.ProductionRepository;
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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @InjectMocks
    private SettlementService settlementService;

    @Mock private InventoryRepository inventoryRepository;
    @Mock private InventoryHistoryWriter inventoryHistoryWriter;
    @Mock private ProductionRepository productionRepository;
    @Mock private ItemRepository itemRepository;
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
        Inventory tireInv  = Inventory.builder().id(100L).warehouse(warehouse).item(itemTire).quantity(100).safetyStock(0).build();
        Inventory frameInv = Inventory.builder().id(101L).warehouse(warehouse).item(itemFrame).quantity(50).safetyStock(0).build();

        given(bomService.getFullBomTree(itemBike.getId(), owner.getId()))
                .willReturn(Map.of(itemTire.getId(), 2L, itemFrame.getId(), 1L));
        given(inventoryRepository.findAllByWarehouseIdAndItemIdInForUpdate(eq(warehouse.getId()), anyList()))
                .willReturn(List.of(tireInv, frameInv));
        given(itemRepository.findAllById(anyList()))
                .willReturn(List.of(itemTire, itemFrame));
        given(productionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(settlementRepository.save(any(Settlement.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        Settlement result = settlementService.settle(record);

        // then
        assertThat(result.getResult()).isEqualTo(SettlementResult.SUCCESS);
        assertThat(result.getAnomalyDetail()).isNull();
        then(inventoryHistoryWriter).should(times(2)).save(any(), any(), anyInt(), any());
    }

    @Test
    @DisplayName("결산 성공 - BOM 없는 품목 (부품/반제품), 재고 차감 없이 SUCCESS")
    void settle_noBom_success() {
        // given: PART 타입 품목은 BOM이 없으므로 빈 Map 반환 → 차감 없이 SUCCESS
        ProductionRecord partRecord = ProductionRecord.builder()
                .id(2L).warehouse(warehouse).item(itemTire).quantity(10)
                .status(ProductionStatus.PENDING).build();

        given(bomService.getFullBomTree(itemTire.getId(), owner.getId())).willReturn(Map.of());
        given(productionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(settlementRepository.save(any(Settlement.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        Settlement result = settlementService.settle(partRecord);

        // then
        assertThat(result.getResult()).isEqualTo(SettlementResult.SUCCESS);
        then(inventoryHistoryWriter).should(never()).save(any(), any(), anyInt(), any());
    }

    @Test
    @DisplayName("결산 이상 - 재고 항목 자체 없음 (inventory 미등록), ANOMALY")
    void settle_inventoryNotFound() throws Exception {
        // given: 타이어 재고 항목 자체가 DB에 없음 → inventoryMap에 없으면 ANOMALY
        given(bomService.getFullBomTree(itemBike.getId(), owner.getId()))
                .willReturn(Map.of(itemTire.getId(), 2L));
        given(inventoryRepository.findAllByWarehouseIdAndItemIdInForUpdate(eq(warehouse.getId()), anyList()))
                .willReturn(List.of()); // 재고 항목 없음
        given(itemRepository.findAllById(anyList()))
                .willReturn(List.of(itemTire));
        given(objectMapper.writeValueAsString(any())).willReturn("{\"TIRE-001\":{\"required\":10,\"stock\":0}}");
        given(productionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(settlementRepository.save(any(Settlement.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        Settlement result = settlementService.settle(record);

        // then
        assertThat(result.getResult()).isEqualTo(SettlementResult.ANOMALY);
        then(inventoryHistoryWriter).should(never()).save(any(), any(), anyInt(), any());
    }

    @Test
    @DisplayName("결산 이상 - 부품 재고 부족, ANOMALY (가능한 수량 차감 후 기록)")
    void settle_anomaly() throws Exception {
        // given: 자전거 BOM = { 타이어: 2 }, 생산 5개
        // 필요: 타이어 10개, 재고: 3개 → ANOMALY, 가능한 3개 전부 차감
        Inventory tireInv = Inventory.builder().id(100L).warehouse(warehouse).item(itemTire).quantity(3).safetyStock(0).build();

        given(bomService.getFullBomTree(itemBike.getId(), owner.getId()))
                .willReturn(Map.of(itemTire.getId(), 2L));
        given(inventoryRepository.findAllByWarehouseIdAndItemIdInForUpdate(eq(warehouse.getId()), anyList()))
                .willReturn(List.of(tireInv));
        given(itemRepository.findAllById(anyList()))
                .willReturn(List.of(itemTire));
        given(objectMapper.writeValueAsString(any())).willReturn("{\"TIRE-001\":{\"required\":10,\"stock\":3}}");
        given(productionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(settlementRepository.save(any(Settlement.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        Settlement result = settlementService.settle(record);

        // then
        assertThat(result.getResult()).isEqualTo(SettlementResult.ANOMALY);
        assertThat(result.getAnomalyDetail()).isNotNull();
        assertThat(tireInv.getQuantity()).isEqualTo(0); // 가능한 수량(3개) 전부 차감됨
        then(inventoryHistoryWriter).should(times(1)).save(any(), any(), anyInt(), any());
    }

    @Test
    @DisplayName("결산 실패 - BOM 탐색 오류 (시스템 문제), FAILED")
    void settle_failed_bomError() throws Exception {
        // given: BOM 탐색 중 ImsException 발생 (깊이 초과 등)
        given(bomService.getFullBomTree(itemBike.getId(), owner.getId()))
                .willThrow(new ImsException(ErrorCode.BOM_DEPTH_EXCEEDED));
        given(objectMapper.writeValueAsString(any())).willReturn("{\"error\":\"BOM 깊이 초과\"}");
        given(productionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(settlementRepository.save(any(Settlement.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        Settlement result = settlementService.settle(record);

        // then
        assertThat(result.getResult()).isEqualTo(SettlementResult.FAILED);
        assertThat(result.getAnomalyDetail()).contains("error");
        then(inventoryHistoryWriter).should(never()).save(any(), any(), anyInt(), any());
    }

    @Test
    @DisplayName("결산 방어 - PENDING 아닌 레코드 결산 시도 시 예외")
    void settle_alreadySettled() {
        // given: 이미 SETTLED 상태인 레코드
        ProductionRecord settled = ProductionRecord.builder()
                .id(2L).warehouse(warehouse).item(itemBike).quantity(5)
                .status(ProductionStatus.SETTLED).build();

        // when & then: 재결산 시도 → PRODUCTION_ALREADY_SETTLED 예외
        assertThatThrownBy(() -> settlementService.settle(settled))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCTION_ALREADY_SETTLED);
        then(bomService).should(never()).getFullBomTree(any(), any());
        then(settlementRepository).should(never()).save(any());
    }
}
