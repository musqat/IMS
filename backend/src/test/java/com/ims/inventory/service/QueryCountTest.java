package com.ims.inventory.service;

import com.ims.inventory.entity.Inventory;
import com.ims.inventory.repository.InventoryHistoryRepository;
import com.ims.inventory.repository.InventoryRepository;
import com.ims.item.entity.Bom;
import com.ims.item.entity.Item;
import com.ims.item.entity.ItemType;
import com.ims.item.repository.BomRepository;
import com.ims.item.repository.ItemRepository;
import com.ims.production.repository.ProductionRepository;
import com.ims.production.repository.SettlementRepository;
import com.ims.user.entity.User;
import com.ims.user.repository.UserRepository;
import com.ims.warehouse.entity.Warehouse;
import com.ims.warehouse.repository.WarehouseRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 조회 경로의 쿼리 수 측정 — 데이터 규모를 늘려도 쿼리 수가 고정인지 본다
 * 확인 방법 : @EntityGraph를 떼면 실패한다
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
class QueryCountTest {

    @MockitoBean RedisConnectionFactory redisConnectionFactory;
    @MockitoBean ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;

    @Autowired InventoryService inventoryService;
    @Autowired EntityManagerFactory emf;

    @Autowired UserRepository userRepository;
    @Autowired WarehouseRepository warehouseRepository;
    @Autowired ItemRepository itemRepository;
    @Autowired BomRepository bomRepository;
    @Autowired InventoryRepository inventoryRepository;
    @Autowired InventoryHistoryRepository inventoryHistoryRepository;
    @Autowired ProductionRepository productionRepository;
    @Autowired SettlementRepository settlementRepository;

    private Long ownerId;
    private Long warehouseId;
    private final List<Long> productIds = new ArrayList<>();

    /** 측정 구간의 JDBC 문장 수 */
    private long countQueries(Runnable block) {
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        long before = stats.getPrepareStatementCount();
        block.run();
        return stats.getPrepareStatementCount() - before;
    }

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
        productIds.clear();
    }

    /**
     * 완성품 N개, 완성품마다 부품 partsPerProduct 개.
     * 부품 재고는 필요량보다 적게 넣어 부족 분석에 전부 걸리게 한다
     */
    private void seed(int productCount, int partsPerProduct) {
        User owner = userRepository.save(User.builder()
                .email("qc@test.com").password("pw")
                .companyName("측정").companyCode("QC001").build());
        Warehouse warehouse = warehouseRepository.save(
                Warehouse.builder().owner(owner).name("측정창고").build());

        for (int p = 0; p < productCount; p++) {
            Item product = itemRepository.save(Item.builder()
                    .owner(owner).itemCode("PROD-" + p).name("완성품" + p)
                    .type(ItemType.PRODUCT).build());
            productIds.add(product.getId());

            for (int c = 0; c < partsPerProduct; c++) {
                Item part = itemRepository.save(Item.builder()
                        .owner(owner).itemCode("PART-" + p + "-" + c).name("부품" + p + "-" + c)
                        .type(ItemType.PART).build());
                bomRepository.save(Bom.builder().parent(product).child(part).quantity(2).build());
                inventoryRepository.save(Inventory.builder()
                        .warehouse(warehouse).item(part)
                        .quantity(1).safetyStock(0).build());
            }
        }
        ownerId = owner.getId();
        warehouseId = warehouse.getId();
    }

    @Test
    @DisplayName("부족 분석 - 완성품이 1개에서 20개로 늘어도 쿼리 수가 같다")
    void shortageAnalysis_queryCountIsFlat() {
        // given - 완성품 1개
        seed(1, 3);
        long withOneProduct = countQueries(() -> inventoryService.getShortageAnalysis(ownerId, warehouseId));

        // given - 완성품 20개
        setUp();
        seed(20, 3);
        long withTwentyProducts = countQueries(() -> inventoryService.getShortageAnalysis(ownerId, warehouseId));

        System.out.printf("[측정] 부족 분석  완성품 1개=%d  완성품 20개=%d%n",
                withOneProduct, withTwentyProducts);

        // then - 완성품 수에 비례해 늘지 않는다
        assertThat(withTwentyProducts).isEqualTo(withOneProduct);
    }

    @Test
    @DisplayName("최대 생산량 - BOM 부품이 3개에서 30개로 늘어도 쿼리 수가 같다")
    void maxProducible_queryCountIsFlat() {
        // given - 부품 3개
        seed(1, 3);
        Long productId = productIds.get(0);
        long withThreeParts = countQueries(() -> inventoryService.calcMaxProducible(ownerId, warehouseId, productId));

        // given - 부품 30개
        setUp();
        seed(1, 30);
        Long biggerProductId = productIds.get(0);
        long withThirtyParts = countQueries(() -> inventoryService.calcMaxProducible(ownerId, warehouseId, biggerProductId));

        System.out.printf("[측정] 최대 생산량  부품 3개=%d  부품 30개=%d%n",
                withThreeParts, withThirtyParts);

        // then - BOM 크기에 비례해 늘지 않는다
        assertThat(withThirtyParts).isEqualTo(withThreeParts);
    }

    @Test
    @DisplayName("재고 목록 - 품목이 5개에서 50개로 늘어도 쿼리 수가 같다")
    void inventoryList_queryCountIsFlat() {
        // given - 품목 5개
        seed(1, 5);
        long withFiveItems = countQueries(
                () -> inventoryService.getInventories(ownerId, warehouseId, null, PageRequest.of(0, 100)));

        // given - 품목 50개
        setUp();
        seed(1, 50);
        long withFiftyItems = countQueries(
                () -> inventoryService.getInventories(ownerId, warehouseId, null, PageRequest.of(0, 100)));

        System.out.printf("[측정] 재고 목록  품목 5개=%d  품목 50개=%d%n",
                withFiveItems, withFiftyItems);

        // then - 품목 수만큼 item 조회가 따라붙지 않는다 (@EntityGraph)
        assertThat(withFiftyItems).isEqualTo(withFiveItems);
    }
}
