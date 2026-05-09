package com.ims.global.scheduler;

import com.ims.inventory.entity.Inventory;
import com.ims.inventory.repository.InventoryHistoryRepository;
import com.ims.inventory.repository.InventoryRepository;
import com.ims.item.entity.Bom;
import com.ims.item.entity.Item;
import com.ims.item.entity.ItemType;
import com.ims.item.repository.BomRepository;
import com.ims.item.repository.ItemRepository;
import com.ims.production.entity.ProductionRecord;
import com.ims.production.entity.ProductionStatus;
import com.ims.production.entity.SettlementResult;
import com.ims.production.repository.ProductionRepository;
import com.ims.production.repository.SettlementRepository;
import com.ims.user.entity.User;
import com.ims.user.repository.UserRepository;
import com.ims.warehouse.entity.Warehouse;
import com.ims.warehouse.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class SettlementBatchIntegrationTest {

    // Redis 연결 없이 컨텍스트 로드 가능하도록 Mock 처리
    // Spring Boot 3.x Reactive Redis 자동 설정도 함께 mock 필요
    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;
    @MockitoBean
    private ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;

    @Autowired JobLauncher jobLauncher;
    @Autowired Job settlementJob;
    @Autowired ProductionRepository productionRepository;
    @Autowired SettlementRepository settlementRepository;
    @Autowired InventoryRepository inventoryRepository;
    @Autowired InventoryHistoryRepository inventoryHistoryRepository;
    @Autowired ItemRepository itemRepository;
    @Autowired BomRepository bomRepository;
    @Autowired UserRepository userRepository;
    @Autowired WarehouseRepository warehouseRepository;

    private User owner;
    private Warehouse warehouse;
    private Item itemBike;
    private Item itemTire;

    @BeforeEach
    void setUp() {
        settlementRepository.deleteAll();
        productionRepository.deleteAll();
        inventoryHistoryRepository.deleteAll();
        inventoryRepository.deleteAll();
        bomRepository.deleteAll();
        itemRepository.deleteAll();
        warehouseRepository.deleteAll();
        userRepository.deleteAll();

        owner = userRepository.save(User.builder()
                .email("test@test.com").password("pw").companyName("테스트").companyCode("TC001").build());
        warehouse = warehouseRepository.save(Warehouse.builder()
                .owner(owner).name("서울창고").build());
        itemBike = itemRepository.save(Item.builder()
                .owner(owner).itemCode("BIKE-001").name("A자전거").type(ItemType.PRODUCT).build());
        itemTire = itemRepository.save(Item.builder()
                .owner(owner).itemCode("TIRE-001").name("타이어").type(ItemType.PART).build());
        bomRepository.save(Bom.builder()
                .parent(itemBike).child(itemTire).quantity(2).build());
        inventoryRepository.save(Inventory.builder()
                .warehouse(warehouse).item(itemTire).quantity(100).safetyStock(0).build());
    }

    @Test
    @DisplayName("배치 실행 - PENDING → SETTLED, Settlement(SUCCESS) 생성")
    void batch_pendingToSettled() throws Exception {
        // given: 생산 5개 (타이어 10개 필요, 재고 100개)
        ProductionRecord record = productionRepository.save(ProductionRecord.builder()
                .warehouse(warehouse).item(itemBike).quantity(5)
                .status(ProductionStatus.PENDING).build());

        // when
        JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        JobExecution execution = jobLauncher.run(settlementJob, params);

        // then
        assertThat(execution.getStatus().isUnsuccessful()).isFalse();
        ProductionRecord settled = productionRepository.findById(record.getId()).orElseThrow();
        assertThat(settled.getStatus()).isEqualTo(ProductionStatus.SETTLED);
        assertThat(settlementRepository.findByProductionRecordId(record.getId()).get().getResult())
                .isEqualTo(SettlementResult.SUCCESS);
    }

    @Test
    @DisplayName("배치 실행 - 재고 부족 시 ANOMALY")
    void batch_anomaly() throws Exception {
        // given: 생산 100개 (타이어 200개 필요, 재고 100개)
        ProductionRecord record = productionRepository.save(ProductionRecord.builder()
                .warehouse(warehouse).item(itemBike).quantity(100)
                .status(ProductionStatus.PENDING).build());

        // when
        JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis() + 1)
                .toJobParameters();
        jobLauncher.run(settlementJob, params);

        // then
        assertThat(settlementRepository.findByProductionRecordId(record.getId()).get().getResult())
                .isEqualTo(SettlementResult.ANOMALY);
    }

    @Test
    @DisplayName("배치 실행 - BOM 순환 참조 시 FAILED (시스템 오류)")
    void batch_failed_bomDepthExceeded() throws Exception {
        // given: 깊이 20단계 초과 BOM 구조 생성 → BomService가 BOM_DEPTH_EXCEEDED 예외 발생
        // 단순화: itemBike → itemTire → itemBike 순환 참조를 DB에 직접 삽입
        // (BomService 검증을 우회하여 Repository 레벨에서 직접 저장)
        bomRepository.save(Bom.builder()
                .parent(itemTire).child(itemBike).quantity(1).build()); // 타이어 → 자전거 역방향 추가 → 순환

        ProductionRecord record = productionRepository.save(ProductionRecord.builder()
                .warehouse(warehouse).item(itemBike).quantity(1)
                .status(ProductionStatus.PENDING).build());

        // when
        JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis() + 2)
                .toJobParameters();
        jobLauncher.run(settlementJob, params);

        // then: 순환 참조로 BOM 탐색 실패 → FAILED
        assertThat(settlementRepository.findByProductionRecordId(record.getId()).get().getResult())
                .isEqualTo(SettlementResult.FAILED);
    }
}
