package com.ims.production.repository;

import com.ims.global.config.JpaAuditingConfig;
import com.ims.item.entity.Item;
import com.ims.item.entity.ItemType;
import com.ims.item.repository.ItemRepository;
import com.ims.production.entity.ProductionRecord;
import com.ims.production.entity.ProductionStatus;
import com.ims.production.entity.Settlement;
import com.ims.production.entity.SettlementResult;
import com.ims.user.entity.User;
import com.ims.user.repository.UserRepository;
import com.ims.warehouse.entity.Warehouse;
import com.ims.warehouse.entity.WarehouseShare;
import com.ims.warehouse.entity.WarehouseShare.SharePermission;
import com.ims.warehouse.repository.WarehouseRepository;
import com.ims.warehouse.repository.WarehouseShareRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(JpaAuditingConfig.class)
class ProductionRepositoryTest {

    @Autowired ProductionRepository productionRepository;
    @Autowired SettlementRepository settlementRepository;
    @Autowired WarehouseRepository warehouseRepository;
    @Autowired WarehouseShareRepository warehouseShareRepository;
    @Autowired ItemRepository itemRepository;
    @Autowired UserRepository userRepository;

    private User owner;
    private Warehouse warehouse;
    private Item item;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(User.builder()
                .email("owner@test.com").password("pw").companyName("테스트회사").companyCode("TC001").build());
        warehouse = warehouseRepository.save(Warehouse.builder()
                .owner(owner).name("서울창고").build());
        item = itemRepository.save(Item.builder()
                .owner(owner).itemCode("BIKE-001").name("로드바이크").type(ItemType.PRODUCT).build());
    }

    @Test
    @DisplayName("창고 기준 생산 기록 페이징 조회")
    void findAllByWarehouseId_paging() {
        // given
        productionRepository.save(ProductionRecord.builder()
                .warehouse(warehouse).item(item).quantity(10).status(ProductionStatus.PENDING).build());
        productionRepository.save(ProductionRecord.builder()
                .warehouse(warehouse).item(item).quantity(5).status(ProductionStatus.SETTLED).build());

        // when
        Page<ProductionRecord> result = productionRepository.findAllByWarehouseId(
                warehouse.getId(), PageRequest.of(0, 10));

        // then
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("PENDING 상태 레코드만 조회 - 배치 Reader용")
    void findAllByStatus_pending() {
        // given
        productionRepository.save(ProductionRecord.builder()
                .warehouse(warehouse).item(item).quantity(10).status(ProductionStatus.PENDING).build());
        productionRepository.save(ProductionRecord.builder()
                .warehouse(warehouse).item(item).quantity(5).status(ProductionStatus.SETTLED).build());
        productionRepository.save(ProductionRecord.builder()
                .warehouse(warehouse).item(item).quantity(3).status(ProductionStatus.CANCELLED).build());

        // when
        List<ProductionRecord> result = productionRepository.findAllByStatus(ProductionStatus.PENDING);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("생산 기록 ID로 결산 조회")
    void findByProductionRecordId_success() {
        // given
        ProductionRecord record = productionRepository.save(ProductionRecord.builder()
                .warehouse(warehouse).item(item).quantity(10).status(ProductionStatus.SETTLED).build());
        settlementRepository.save(Settlement.builder()
                .productionRecord(record).result(SettlementResult.SUCCESS).build());

        // when
        Optional<Settlement> result = settlementRepository.findByProductionRecordId(record.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getResult()).isEqualTo(SettlementResult.SUCCESS);
    }

    @Test
    @DisplayName("결산 없는 생산 기록 조회 - empty")
    void findByProductionRecordId_notFound() {
        // given
        ProductionRecord record = productionRepository.save(ProductionRecord.builder()
                .warehouse(warehouse).item(item).quantity(10).status(ProductionStatus.PENDING).build());

        // when
        Optional<Settlement> result = settlementRepository.findByProductionRecordId(record.getId());

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("상태 필터 조회 - PENDING만 반환")
    void findAllByWarehouseIdInAndStatus_filterByStatus() {
        // given
        productionRepository.save(ProductionRecord.builder()
                .warehouse(warehouse).item(item).quantity(10).status(ProductionStatus.PENDING).build());
        productionRepository.save(ProductionRecord.builder()
                .warehouse(warehouse).item(item).quantity(5).status(ProductionStatus.SETTLED).build());
        productionRepository.save(ProductionRecord.builder()
                .warehouse(warehouse).item(item).quantity(3).status(ProductionStatus.CANCELLED).build());

        // when — 소유 창고 ID 직접 전달
        Page<ProductionRecord> result = productionRepository.findAllByWarehouseIdInAndStatus(
                List.of(warehouse.getId()), ProductionStatus.PENDING, PageRequest.of(0, 10));

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("상태 필터 조회 - 공유 창고도 포함")
    void findAllByWarehouseIdInAndStatus_includesSharedWarehouse() {
        // given
        User guest = userRepository.save(User.builder()
                .email("guest@test.com").password("pw").companyName("게스트회사").companyCode("GC001").build());
        warehouseShareRepository.save(WarehouseShare.builder()
                .warehouse(warehouse).sharedWith(guest).permission(SharePermission.VIEW).build());
        productionRepository.save(ProductionRecord.builder()
                .warehouse(warehouse).item(item).quantity(7).status(ProductionStatus.PENDING).build());
        productionRepository.save(ProductionRecord.builder()
                .warehouse(warehouse).item(item).quantity(3).status(ProductionStatus.SETTLED).build());

        // when — 공유받은 창고 ID를 서비스가 미리 수집해서 전달하는 시나리오
        Page<ProductionRecord> result = productionRepository.findAllByWarehouseIdInAndStatus(
                List.of(warehouse.getId()), ProductionStatus.PENDING, PageRequest.of(0, 10));

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getQuantity()).isEqualTo(7);
    }

    @Test
    @DisplayName("상태별 카운트 집계 - 각 상태 건수 정확히 반환")
    void countGroupByStatusInWarehouses_returnsCorrectCounts() {
        // given
        productionRepository.save(ProductionRecord.builder()
                .warehouse(warehouse).item(item).quantity(10).status(ProductionStatus.PENDING).build());
        productionRepository.save(ProductionRecord.builder()
                .warehouse(warehouse).item(item).quantity(5).status(ProductionStatus.PENDING).build());
        productionRepository.save(ProductionRecord.builder()
                .warehouse(warehouse).item(item).quantity(3).status(ProductionStatus.SETTLED).build());
        productionRepository.save(ProductionRecord.builder()
                .warehouse(warehouse).item(item).quantity(2).status(ProductionStatus.CANCELLED).build());

        // when
        List<Object[]> rows = productionRepository.countGroupByStatusInWarehouses(List.of(warehouse.getId()));
        Map<ProductionStatus, Long> counts = rows.stream()
                .collect(Collectors.toMap(r -> (ProductionStatus) r[0], r -> (Long) r[1]));

        // then
        assertThat(counts.get(ProductionStatus.PENDING)).isEqualTo(2L);
        assertThat(counts.get(ProductionStatus.SETTLED)).isEqualTo(1L);
        assertThat(counts.get(ProductionStatus.CANCELLED)).isEqualTo(1L);
    }

    @Test
    @DisplayName("상태별 카운트 집계 - 다른 창고는 제외")
    void countGroupByStatusInWarehouses_excludesOtherWarehouses() {
        // given
        productionRepository.save(ProductionRecord.builder()
                .warehouse(warehouse).item(item).quantity(10).status(ProductionStatus.PENDING).build());

        // when — 빈 창고 ID 목록 → 결과 없음
        List<Object[]> rows = productionRepository.countGroupByStatusInWarehouses(List.of(-1L));

        // then
        assertThat(rows).isEmpty();
    }

    @Test
    @DisplayName("ANOMALY 결산 건수 - 창고 ID 목록 기준 정확히 반환")
    void countByResultAndWarehouseIdIn_anomalyCount() {
        // given
        ProductionRecord r1 = productionRepository.save(ProductionRecord.builder()
                .warehouse(warehouse).item(item).quantity(10).status(ProductionStatus.SETTLED).build());
        ProductionRecord r2 = productionRepository.save(ProductionRecord.builder()
                .warehouse(warehouse).item(item).quantity(5).status(ProductionStatus.SETTLED).build());
        ProductionRecord r3 = productionRepository.save(ProductionRecord.builder()
                .warehouse(warehouse).item(item).quantity(3).status(ProductionStatus.SETTLED).build());

        settlementRepository.save(Settlement.builder().productionRecord(r1).result(SettlementResult.ANOMALY).build());
        settlementRepository.save(Settlement.builder().productionRecord(r2).result(SettlementResult.ANOMALY).build());
        settlementRepository.save(Settlement.builder().productionRecord(r3).result(SettlementResult.SUCCESS).build());

        // when
        long count = settlementRepository.countByResultAndProductionRecordWarehouseIdIn(
                SettlementResult.ANOMALY, List.of(warehouse.getId()));

        // then
        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("ANOMALY 결산 건수 - 다른 창고는 제외")
    void countByResultAndWarehouseIdIn_excludesOtherWarehouses() {
        // given
        ProductionRecord record = productionRepository.save(ProductionRecord.builder()
                .warehouse(warehouse).item(item).quantity(10).status(ProductionStatus.SETTLED).build());
        settlementRepository.save(Settlement.builder().productionRecord(record).result(SettlementResult.ANOMALY).build());

        // when — 접근 불가 창고 ID 목록
        long count = settlementRepository.countByResultAndProductionRecordWarehouseIdIn(
                SettlementResult.ANOMALY, List.of(-1L));

        // then
        assertThat(count).isZero();
    }
}
