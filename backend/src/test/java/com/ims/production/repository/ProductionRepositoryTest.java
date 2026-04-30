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
import com.ims.warehouse.repository.WarehouseRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(JpaAuditingConfig.class)
class ProductionRepositoryTest {

    @Autowired ProductionRepository productionRepository;
    @Autowired SettlementRepository settlementRepository;
    @Autowired WarehouseRepository warehouseRepository;
    @Autowired ItemRepository itemRepository;
    @Autowired UserRepository userRepository;

    private Warehouse warehouse;
    private Item item;

    @BeforeEach
    void setUp() {
        User owner = userRepository.save(User.builder()
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
}
