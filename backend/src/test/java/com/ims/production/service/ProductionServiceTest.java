package com.ims.production.service;

import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.global.support.DomainValidator;
import com.ims.item.entity.Item;
import com.ims.item.entity.ItemType;
import com.ims.production.dto.request.ProductionCreateRequest;
import com.ims.production.dto.response.ProductionCountsResponse;
import com.ims.production.dto.request.ProductionUpdateRequest;
import com.ims.production.dto.request.SettlementUpdateRequest;
import com.ims.production.dto.response.ProductionResponse;
import com.ims.production.entity.ProductionRecord;
import com.ims.production.entity.ProductionStatus;
import com.ims.production.entity.Settlement;
import com.ims.production.entity.SettlementResult;
import com.ims.production.repository.ProductionRepository;
import com.ims.production.repository.SettlementRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ProductionServiceTest {

    @InjectMocks
    private ProductionService productionService;

    @Mock private ProductionRepository productionRepository;
    @Mock private SettlementRepository settlementRepository;
    @Mock private DomainValidator domainValidator;
    @Mock private SettlementService settlementService;
    @Mock private WarehouseShareService warehouseShareService;

    private User owner;
    private Warehouse warehouse;
    private Item itemBike;
    private Item itemTire;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).email("test@test.com").password("pw").companyName("테스트").companyCode("TC001").build();
        warehouse = Warehouse.builder().id(1L).owner(owner).name("서울창고").build();
        itemBike = Item.builder().id(10L).owner(owner).itemCode("BIKE-001").name("A자전거").type(ItemType.PRODUCT).build();
        itemTire = Item.builder().id(20L).owner(owner).itemCode("TIRE-001").name("타이어").type(ItemType.PART).build();
    }

    @Test
    @DisplayName("생산 기록 등록 성공")
    void createRecord_success() {
        // given
        ProductionCreateRequest request = new ProductionCreateRequest(itemBike.getId(), 10);
        ProductionRecord saved = ProductionRecord.builder()
                .id(1L).warehouse(warehouse).item(itemBike).quantity(10).status(ProductionStatus.PENDING).build();

        given(domainValidator.getOwnedWarehouse(owner.getId(), warehouse.getId())).willReturn(warehouse);
        given(domainValidator.getOwnedItem(owner.getId(), itemBike.getId())).willReturn(itemBike);
        given(productionRepository.save(any(ProductionRecord.class))).willReturn(saved);

        // when
        ProductionResponse result = productionService.createRecord(owner.getId(), warehouse.getId(), request);

        // then
        assertThat(result.status()).isEqualTo(ProductionStatus.PENDING);
        then(productionRepository).should().save(any(ProductionRecord.class));
    }

    @Test
    @DisplayName("생산 기록 등록 실패 - 창고 소유자 아님")
    void createRecord_warehouseNotOwned() {
        // given
        ProductionCreateRequest request = new ProductionCreateRequest(itemBike.getId(), 10);
        given(domainValidator.getOwnedWarehouse(999L, warehouse.getId())).willThrow(new ImsException(ErrorCode.WAREHOUSE_NOT_OWNED));

        // when & then
        assertThatThrownBy(() -> productionService.createRecord(999L, warehouse.getId(), request))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WAREHOUSE_NOT_OWNED);
    }

    @Test
    @DisplayName("생산 기록 등록 실패 - 품목 소유자 아님")
    void createRecord_itemNotOwned() {
        // given
        ProductionCreateRequest request = new ProductionCreateRequest(itemBike.getId(), 10);
        given(domainValidator.getOwnedWarehouse(owner.getId(), warehouse.getId())).willReturn(warehouse);
        given(domainValidator.getOwnedItem(owner.getId(), itemBike.getId())).willThrow(new ImsException(ErrorCode.ITEM_NOT_OWNED));

        // when & then
        assertThatThrownBy(() -> productionService.createRecord(owner.getId(), warehouse.getId(), request))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ITEM_NOT_OWNED);
    }

    @Test
    @DisplayName("생산 기록 취소 성공")
    void cancelRecord_success() {
        // given
        ProductionRecord record = ProductionRecord.builder()
                .id(1L).warehouse(warehouse).item(itemBike).quantity(10).status(ProductionStatus.PENDING).build();
        given(productionRepository.findById(1L)).willReturn(Optional.of(record));
        given(domainValidator.getOwnedWarehouse(owner.getId(), warehouse.getId())).willReturn(warehouse);

        // when
        productionService.cancelRecord(owner.getId(), warehouse.getId(), 1L);

        // then
        assertThat(record.getStatus()).isEqualTo(ProductionStatus.CANCELLED);
    }

    @Test
    @DisplayName("생산 기록 취소 실패 - PENDING 아님")
    void cancelRecord_notPending() {
        // given
        ProductionRecord record = ProductionRecord.builder()
                .id(1L).warehouse(warehouse).item(itemBike).quantity(10).status(ProductionStatus.SETTLED).build();
        given(productionRepository.findById(1L)).willReturn(Optional.of(record));
        given(domainValidator.getOwnedWarehouse(owner.getId(), warehouse.getId())).willReturn(warehouse);

        // when & then
        assertThatThrownBy(() -> productionService.cancelRecord(owner.getId(), warehouse.getId(), 1L))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCTION_NOT_CANCELLABLE);
    }

    @Test
    @DisplayName("생산 기록 목록 조회 - 결산 있음")
    void getRecords_withSettlement() {
        // given
        ProductionRecord record = ProductionRecord.builder()
                .id(1L).warehouse(warehouse).item(itemBike).quantity(10).status(ProductionStatus.SETTLED).build();
        Settlement settlement = Settlement.builder()
                .id(1L).productionRecord(record).result(SettlementResult.SUCCESS).build();

        given(domainValidator.getOwnedWarehouse(owner.getId(), warehouse.getId())).willReturn(warehouse);
        given(productionRepository.findAllByWarehouseId(eq(warehouse.getId()), any()))
                .willReturn(new PageImpl<>(List.of(record)));
        // N+1 수정: findAllByProductionRecordIdIn 으로 일괄 조회
        given(settlementRepository.findAllByProductionRecordIdIn(List.of(record.getId())))
                .willReturn(List.of(settlement));

        // when
        Page<ProductionResponse> result = productionService.getRecords(owner.getId(), warehouse.getId(), PageRequest.of(0, 10));

        // then
        assertThat(result.getContent().get(0).settlement().result()).isEqualTo(SettlementResult.SUCCESS);
    }

    @Test
    @DisplayName("생산 기록 목록 조회 - 결산 없음 (PENDING)")
    void getRecords_withoutSettlement() {
        // given
        ProductionRecord record = ProductionRecord.builder()
                .id(1L).warehouse(warehouse).item(itemBike).quantity(10).status(ProductionStatus.PENDING).build();

        given(domainValidator.getOwnedWarehouse(owner.getId(), warehouse.getId())).willReturn(warehouse);
        given(productionRepository.findAllByWarehouseId(eq(warehouse.getId()), any()))
                .willReturn(new PageImpl<>(List.of(record)));
        given(settlementRepository.findAllByProductionRecordIdIn(List.of(record.getId())))
                .willReturn(List.of()); // 결산 없음

        // when
        Page<ProductionResponse> result = productionService.getRecords(owner.getId(), warehouse.getId(), PageRequest.of(0, 10));

        // then
        assertThat(result.getContent().get(0).settlement()).isNull();
    }

    @Test
    @DisplayName("생산 기록 수정 성공 - 수량 변경")
    void updateRecord_success() {
        // given
        ProductionRecord record = ProductionRecord.builder()
                .id(1L).warehouse(warehouse).item(itemBike).quantity(10).status(ProductionStatus.PENDING).build();
        ProductionUpdateRequest request = new ProductionUpdateRequest(30);

        given(domainValidator.getOwnedWarehouse(owner.getId(), warehouse.getId())).willReturn(warehouse);
        given(productionRepository.findById(1L)).willReturn(Optional.of(record));

        // when
        ProductionResponse result = productionService.updateRecord(owner.getId(), warehouse.getId(), 1L, request);

        // then
        assertThat(result.quantity()).isEqualTo(30);
    }

    @Test
    @DisplayName("생산 기록 수정 실패 - PENDING 아님")
    void updateRecord_notPending() {
        // given
        ProductionRecord record = ProductionRecord.builder()
                .id(1L).warehouse(warehouse).item(itemBike).quantity(10).status(ProductionStatus.SETTLED).build();
        ProductionUpdateRequest request = new ProductionUpdateRequest(30);

        given(domainValidator.getOwnedWarehouse(owner.getId(), warehouse.getId())).willReturn(warehouse);
        given(productionRepository.findById(1L)).willReturn(Optional.of(record));

        // when & then
        assertThatThrownBy(() -> productionService.updateRecord(owner.getId(), warehouse.getId(), 1L, request))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCTION_NOT_MODIFIABLE);
    }

    @Test
    @DisplayName("생산 기록 수정 실패 - 소유자 아님")
    void updateRecord_notOwner() {
        // given
        ProductionUpdateRequest request = new ProductionUpdateRequest(30);
        given(domainValidator.getOwnedWarehouse(999L, warehouse.getId())).willThrow(new ImsException(ErrorCode.WAREHOUSE_NOT_OWNED));

        // when & then
        assertThatThrownBy(() -> productionService.updateRecord(999L, warehouse.getId(), 1L, request))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WAREHOUSE_NOT_OWNED);
    }

    @Test
    @DisplayName("강제 결산 성공")
    void forceSettle_success() {
        // given
        ProductionRecord record = ProductionRecord.builder()
                .id(1L).warehouse(warehouse).item(itemBike).quantity(10).status(ProductionStatus.PENDING).build();
        Settlement settlement = Settlement.builder()
                .id(1L).productionRecord(record).result(SettlementResult.SUCCESS).build();

        given(domainValidator.getOwnedWarehouse(owner.getId(), warehouse.getId())).willReturn(warehouse);
        given(productionRepository.findWithDetailsById(1L)).willReturn(Optional.of(record));
        given(productionRepository.findById(1L)).willReturn(Optional.of(record)); // re-fetch after settle
        given(settlementService.settle(record)).willAnswer(inv -> {
            record.settle(); // 실제 엔티티 상태 변경 시뮬레이션
            return settlement;
        });

        // when
        ProductionResponse result = productionService.forceSettle(owner.getId(), warehouse.getId(), 1L);

        // then
        assertThat(result.status()).isEqualTo(ProductionStatus.SETTLED);
        assertThat(result.settlement().result()).isEqualTo(SettlementResult.SUCCESS);
        then(settlementService).should().settle(record);
    }

    @Test
    @DisplayName("강제 결산 실패 - PENDING 아님")
    void forceSettle_notPending() {
        // given
        ProductionRecord record = ProductionRecord.builder()
                .id(1L).warehouse(warehouse).item(itemBike).quantity(10).status(ProductionStatus.SETTLED).build();

        given(domainValidator.getOwnedWarehouse(owner.getId(), warehouse.getId())).willReturn(warehouse);
        given(productionRepository.findWithDetailsById(1L)).willReturn(Optional.of(record));

        // when & then
        assertThatThrownBy(() -> productionService.forceSettle(owner.getId(), warehouse.getId(), 1L))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCTION_NOT_MODIFIABLE);
        then(settlementService).should(never()).settle(any());
    }

    @Test
    @DisplayName("결산 수정 성공 - ANOMALY → SUCCESS")
    void updateSettlement_success() {
        // given
        ProductionRecord record = ProductionRecord.builder()
                .id(1L).warehouse(warehouse).item(itemBike).quantity(10).status(ProductionStatus.SETTLED).build();
        Settlement settlement = Settlement.builder()
                .id(1L).productionRecord(record).result(SettlementResult.ANOMALY).build();
        SettlementUpdateRequest request = new SettlementUpdateRequest(SettlementResult.SUCCESS, "재고 보충 후 재확인");

        given(domainValidator.getOwnedWarehouse(owner.getId(), warehouse.getId())).willReturn(warehouse);
        given(productionRepository.findById(1L)).willReturn(Optional.of(record));
        given(settlementRepository.findByProductionRecordId(1L)).willReturn(Optional.of(settlement));

        // when
        ProductionResponse result = productionService.updateSettlement(owner.getId(), warehouse.getId(), 1L, request);

        // then
        assertThat(result.settlement().result()).isEqualTo(SettlementResult.SUCCESS);
        assertThat(result.settlement().memo()).isEqualTo("재고 보충 후 재확인");
    }

    @Test
    @DisplayName("결산 수정 실패 - SETTLED 상태 아님")
    void updateSettlement_notSettled() {
        // given
        ProductionRecord record = ProductionRecord.builder()
                .id(1L).warehouse(warehouse).item(itemBike).quantity(10).status(ProductionStatus.PENDING).build();
        SettlementUpdateRequest request = new SettlementUpdateRequest(SettlementResult.SUCCESS, "메모");

        given(domainValidator.getOwnedWarehouse(owner.getId(), warehouse.getId())).willReturn(warehouse);
        given(productionRepository.findById(1L)).willReturn(Optional.of(record));

        // when & then
        assertThatThrownBy(() -> productionService.updateSettlement(owner.getId(), warehouse.getId(), 1L, request))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCTION_NOT_SETTLED);
    }

    @Test
    @DisplayName("생산 기록 취소 실패 - 창고 소유자 아님")
    void cancelRecord_warehouseNotOwned() {
        // given
        given(domainValidator.getOwnedWarehouse(999L, warehouse.getId()))
                .willThrow(new ImsException(ErrorCode.WAREHOUSE_NOT_OWNED));

        // when & then
        assertThatThrownBy(() -> productionService.cancelRecord(999L, warehouse.getId(), 1L))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WAREHOUSE_NOT_OWNED);
        then(productionRepository).should(never()).findById(any());
    }

    @Test
    @DisplayName("강제 결산 실패 - 창고 소유자 아님")
    void forceSettle_warehouseNotOwned() {
        // given
        given(domainValidator.getOwnedWarehouse(999L, warehouse.getId()))
                .willThrow(new ImsException(ErrorCode.WAREHOUSE_NOT_OWNED));

        // when & then
        assertThatThrownBy(() -> productionService.forceSettle(999L, warehouse.getId(), 1L))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WAREHOUSE_NOT_OWNED);
        then(settlementService).should(never()).settle(any());
    }

    @Test
    @DisplayName("상태별 카운트 집계 - PENDING 2건, SETTLED 1건, ANOMALY 1건")
    void getStatusCounts_returnsCorrectCounts() {
        // given
        List<Object[]> statusRows = List.of(
                new Object[]{ProductionStatus.PENDING, 2L},
                new Object[]{ProductionStatus.SETTLED, 1L},
                new Object[]{ProductionStatus.CANCELLED, 0L}
        );
        given(warehouseShareService.getAccessibleWarehouseIds(owner.getId())).willReturn(List.of(warehouse.getId()));
        given(productionRepository.countGroupByStatusInWarehouses(anyList())).willReturn(statusRows);
        given(settlementRepository.countByResultAndProductionRecordWarehouseIdIn(
                eq(SettlementResult.ANOMALY), anyList())).willReturn(1L);

        // when
        ProductionCountsResponse counts = productionService.getStatusCounts(owner.getId());

        // then
        assertThat(counts.pending()).isEqualTo(2L);
        assertThat(counts.settled()).isEqualTo(1L);
        assertThat(counts.cancelled()).isEqualTo(0L);
        assertThat(counts.anomaly()).isEqualTo(1L);
        assertThat(counts.total()).isEqualTo(3L);
    }

    @Test
    @DisplayName("상태별 카운트 집계 - 기록 없으면 전부 0")
    void getStatusCounts_emptyReturnsZeros() {
        // given
        given(warehouseShareService.getAccessibleWarehouseIds(owner.getId())).willReturn(List.of(warehouse.getId()));
        given(productionRepository.countGroupByStatusInWarehouses(anyList())).willReturn(List.of());
        given(settlementRepository.countByResultAndProductionRecordWarehouseIdIn(
                eq(SettlementResult.ANOMALY), anyList())).willReturn(0L);

        // when
        ProductionCountsResponse counts = productionService.getStatusCounts(owner.getId());

        // then
        assertThat(counts.pending()).isZero();
        assertThat(counts.settled()).isZero();
        assertThat(counts.cancelled()).isZero();
        assertThat(counts.anomaly()).isZero();
        assertThat(counts.total()).isZero();
    }

    @Test
    @DisplayName("결산 수정 실패 - Settlement 없음")
    void updateSettlement_settlementNotFound() {
        // given: 레코드는 SETTLED, 하지만 연결된 Settlement가 없음
        ProductionRecord record = ProductionRecord.builder()
                .id(1L).warehouse(warehouse).item(itemBike).quantity(10).status(ProductionStatus.SETTLED).build();
        SettlementUpdateRequest request = new SettlementUpdateRequest(SettlementResult.SUCCESS, "메모");

        given(domainValidator.getOwnedWarehouse(owner.getId(), warehouse.getId())).willReturn(warehouse);
        given(productionRepository.findById(1L)).willReturn(Optional.of(record));
        given(settlementRepository.findByProductionRecordId(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productionService.updateSettlement(owner.getId(), warehouse.getId(), 1L, request))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SETTLEMENT_NOT_FOUND);
    }
}
