package com.ims.production.service;

import com.ims.global.exception.ImsException;
import com.ims.item.entity.Item;
import com.ims.item.entity.ItemType;
import com.ims.item.repository.ItemRepository;
import com.ims.production.dto.request.ProductionCreateRequest;
import com.ims.production.dto.response.ProductionResponse;
import com.ims.production.entity.ProductionRecord;
import com.ims.production.entity.ProductionStatus;
import com.ims.production.entity.Settlement;
import com.ims.production.entity.SettlementResult;
import com.ims.production.repository.ProductionRepository;
import com.ims.production.repository.SettlementRepository;
import com.ims.user.entity.User;
import com.ims.warehouse.entity.Warehouse;
import com.ims.warehouse.repository.WarehouseRepository;
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
    @Mock private WarehouseRepository warehouseRepository;
    @Mock private ItemRepository itemRepository;

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

        given(warehouseRepository.findById(warehouse.getId())).willReturn(Optional.of(warehouse));
        given(itemRepository.findById(itemBike.getId())).willReturn(Optional.of(itemBike));
        given(productionRepository.save(any(ProductionRecord.class))).willReturn(saved);

        // when
        ProductionResponse result = productionService.createRecord(owner.getId(), warehouse.getId(), request);

        // then
        assertThat(result.status()).isEqualTo(ProductionStatus.PENDING);
        then(productionRepository).should().save(any(ProductionRecord.class));
    }

    @Test
    @DisplayName("생산 기록 등록 실패 - PRODUCT 타입 아님")
    void createRecord_notFinished() {
        // given
        ProductionCreateRequest request = new ProductionCreateRequest(itemTire.getId(), 10);
        given(warehouseRepository.findById(warehouse.getId())).willReturn(Optional.of(warehouse));
        given(itemRepository.findById(itemTire.getId())).willReturn(Optional.of(itemTire));

        // when & then
        assertThatThrownBy(() -> productionService.createRecord(owner.getId(), warehouse.getId(), request))
                .isInstanceOf(ImsException.class);
    }

    @Test
    @DisplayName("생산 기록 등록 실패 - 소유자 아님")
    void createRecord_notOwner() {
        // given
        ProductionCreateRequest request = new ProductionCreateRequest(itemBike.getId(), 10);
        given(warehouseRepository.findById(warehouse.getId())).willReturn(Optional.of(warehouse));

        // when & then
        assertThatThrownBy(() -> productionService.createRecord(999L, warehouse.getId(), request))
                .isInstanceOf(ImsException.class);
    }

    @Test
    @DisplayName("생산 기록 취소 성공")
    void cancelRecord_success() {
        // given
        ProductionRecord record = ProductionRecord.builder()
                .id(1L).warehouse(warehouse).item(itemBike).quantity(10).status(ProductionStatus.PENDING).build();
        given(productionRepository.findById(1L)).willReturn(Optional.of(record));

        // when
        productionService.cancelRecord(owner.getId(), 1L);

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

        // when & then
        assertThatThrownBy(() -> productionService.cancelRecord(owner.getId(), 1L))
                .isInstanceOf(ImsException.class);
    }

    @Test
    @DisplayName("생산 기록 목록 조회 - 결산 있음")
    void getRecords_withSettlement() {
        // given
        ProductionRecord record = ProductionRecord.builder()
                .id(1L).warehouse(warehouse).item(itemBike).quantity(10).status(ProductionStatus.SETTLED).build();
        Settlement settlement = Settlement.builder()
                .id(1L).productionRecord(record).result(SettlementResult.SUCCESS).build();

        given(warehouseRepository.findById(warehouse.getId())).willReturn(Optional.of(warehouse));
        given(productionRepository.findAllByWarehouseId(eq(warehouse.getId()), any()))
                .willReturn(new PageImpl<>(List.of(record)));
        given(settlementRepository.findByProductionRecordId(record.getId())).willReturn(Optional.of(settlement));

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

        given(warehouseRepository.findById(warehouse.getId())).willReturn(Optional.of(warehouse));
        given(productionRepository.findAllByWarehouseId(eq(warehouse.getId()), any()))
                .willReturn(new PageImpl<>(List.of(record)));
        given(settlementRepository.findByProductionRecordId(record.getId())).willReturn(Optional.empty());

        // when
        Page<ProductionResponse> result = productionService.getRecords(owner.getId(), warehouse.getId(), PageRequest.of(0, 10));

        // then
        assertThat(result.getContent().get(0).settlement()).isNull();
    }
}
