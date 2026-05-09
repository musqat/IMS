package com.ims.inventory.service;

import com.ims.inventory.entity.Inventory;
import com.ims.inventory.repository.InventoryHistoryRepository;
import com.ims.inventory.repository.InventoryRepository;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 재고 차감 비관적 락 동시성 통합 테스트
 * - 여러 스레드가 동시에 같은 재고를 차감할 때 음수 재고가 발생하지 않음을 검증
 */
@SpringBootTest
@ActiveProfiles("test")
class InventoryConcurrencyTest {

    @MockitoBean RedisConnectionFactory redisConnectionFactory;
    @MockitoBean ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;

    @Autowired InventoryRepository inventoryRepository;
    @Autowired InventoryHistoryRepository inventoryHistoryRepository;
    @Autowired UserRepository userRepository;
    @Autowired WarehouseRepository warehouseRepository;
    @Autowired ItemRepository itemRepository;
    @Autowired BomRepository bomRepository;
    @Autowired SettlementRepository settlementRepository;
    @Autowired ProductionRepository productionRepository;
    @Autowired TransactionTemplate txTemplate;

    private Long inventoryId;
    private Long warehouseId;
    private Long itemId;
    private static final int INITIAL_STOCK = 10;
    private static final int DEDUCT_PER_THREAD = 3;
    private static final int THREAD_COUNT = 6; // 총 차감 시도: 18개 > 재고 10개

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

        User owner = userRepository.save(
                User.builder().email("concurrency@test.com").password("pw")
                        .companyName("테스트").companyCode("TC999").build());
        Warehouse warehouse = warehouseRepository.save(
                Warehouse.builder().owner(owner).name("동시성창고").build());
        Item part = itemRepository.save(
                Item.builder().owner(owner).itemCode("P-CONC").name("동시성테스트부품").type(ItemType.PART).build());
        Inventory inv = inventoryRepository.save(
                Inventory.builder().warehouse(warehouse).item(part)
                        .quantity(INITIAL_STOCK).safetyStock(0).build());
        inventoryId = inv.getId();
        warehouseId  = warehouse.getId();
        itemId       = part.getId();
    }

    @Test
    @DisplayName("비관적 락 - 동시 차감 시 재고 음수 미발생 검증")
    void pessimisticLock_preventsNegativeStock() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await(); // 모든 스레드 준비 완료 후 동시 출발
                    txTemplate.execute(status -> {
                        List<Inventory> locked = inventoryRepository
                                .findAllByWarehouseIdAndItemIdInForUpdate(
                                        warehouseId, List.of(itemId));
                        Inventory inv = locked.get(0);
                        if (inv.getQuantity() >= DEDUCT_PER_THREAD) {
                            inv.deduct(DEDUCT_PER_THREAD);
                            inventoryRepository.save(inv);
                            successCount.incrementAndGet();
                        }
                        return null;
                    });
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await(); // 모든 스레드 준비 대기
        start.countDown(); // 동시 출발
        done.await();  // 전체 완료 대기
        executor.shutdown();

        // 최종 재고는 반드시 0 이상
        int finalStock = inventoryRepository.findById(inventoryId)
                .map(Inventory::getQuantity).orElseThrow();
        assertThat(finalStock).isGreaterThanOrEqualTo(0);

        // 성공한 차감 횟수 × DEDUCT_PER_THREAD + 남은 재고 = INITIAL_STOCK
        assertThat(successCount.get() * DEDUCT_PER_THREAD + finalStock).isEqualTo(INITIAL_STOCK);
    }
}
