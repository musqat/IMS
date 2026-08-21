package com.ims.inventory.service;

import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.inventory.dto.request.OutboundRequest;
import com.ims.inventory.entity.Inventory;
import com.ims.inventory.entity.InventoryHistory;
import com.ims.inventory.entity.InventoryHistoryType;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 재고 차감 동시성 통합 테스트
 * - 여러 스레드가 동시에 InventoryService.adjustOut을 호출해도 차감이 유실되지 않음을 검증
 * - 반드시 서비스를 통해 호출한다. 리포지토리로 직접 차감하면 프로덕션 경로를 검증하지 못한다
 * - 제대로 짰는지 확인: 서비스의 락을 떼면 이 테스트가 실패해야 한다
 * - H2(MODE=PostgreSQL)에서 돌기 때문에 락 의미론은 실제 Postgres와 다르다.
 *   Testcontainers로 옮기면 해결되나 CI에 Docker가 필요하다
 */
@SpringBootTest
@ActiveProfiles("test")
class InventoryConcurrencyTest {

    @MockitoBean RedisConnectionFactory redisConnectionFactory;
    @MockitoBean ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;

    @Autowired InventoryService inventoryService;
    @Autowired InventoryRepository inventoryRepository;
    @Autowired InventoryHistoryRepository inventoryHistoryRepository;
    @Autowired UserRepository userRepository;
    @Autowired WarehouseRepository warehouseRepository;
    @Autowired ItemRepository itemRepository;
    @Autowired BomRepository bomRepository;
    @Autowired SettlementRepository settlementRepository;
    @Autowired ProductionRepository productionRepository;

    private Long ownerId;
    private Long inventoryId;
    private Long warehouseId;
    private Long itemId;

    private static final int INITIAL_STOCK = 10;
    private static final int DEDUCT_PER_THREAD = 3;
    private static final int THREAD_COUNT = 6; // 총 차감 시도 18개 > 재고 10개

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

        // adjustOut은 쓰기 권한을 검증한다. 창고 소유자로 호출해야 통과한다
        ownerId     = owner.getId();
        inventoryId = inv.getId();
        warehouseId = warehouse.getId();
        itemId      = part.getId();
    }

    /**
     * 재고 10에 3씩 6스레드 동시 출고 → 성공 3건, 재고부족 3건, 잔여 1
     * 락이 없으면 전부 같은 값을 읽고 서로 덮어써서 성공 6건에 재고 7이 남는다
     */
    @Test
    @DisplayName("동시 출고 - 차감이 유실되지 않는다")
    void adjustOut_concurrent_noLostUpdate() throws InterruptedException {
        ConcurrentResult result = runConcurrently(THREAD_COUNT, this::outbound);

        int expectedSuccess = INITIAL_STOCK / DEDUCT_PER_THREAD;          // 3
        int finalStock = currentStock();

        // 예상 밖 실패를 먼저 본다. 아래 수량이 틀린 원인이 여기 있을 수 있다
        assertThat(result.unexpected()).isEmpty();
        assertThat(result.success()).isEqualTo(expectedSuccess);
        // 재고가 모자라 거부된 건수 — 예상된 실패다
        assertThat(result.insufficient()).isEqualTo(THREAD_COUNT - expectedSuccess);
        assertThat(finalStock).isEqualTo(INITIAL_STOCK - expectedSuccess * DEDUCT_PER_THREAD);
    }

    /**
     * 성공한 출고만 이력을 남긴다
     * 재고 숫자만 맞고 이력이 어긋나면 월/연 평균 분석이 틀어진다
     */
    @Test
    @DisplayName("동시 출고 - 성공 건수만큼만 이력이 남는다")
    void adjustOut_concurrent_historyMatchesStock() throws InterruptedException {
        ConcurrentResult result = runConcurrently(THREAD_COUNT, this::outbound);

        // setUp이 이력을 전부 지우므로 남은 건 이 테스트가 만든 것뿐이다
        List<InventoryHistory> histories = inventoryHistoryRepository.findAll();
        int deltaSum = histories.stream().mapToInt(InventoryHistory::getDelta).sum();
        int finalStock = currentStock();

        assertThat(result.unexpected()).isEmpty();
        assertThat(histories).hasSize(result.success());
        assertThat(histories).allMatch(h -> h.getType() == InventoryHistoryType.OUT);
        assertThat(deltaSum).isEqualTo(-result.success() * DEDUCT_PER_THREAD);
        // 재고와 이력을 묶는 단언. 둘 중 하나만 검증하면 나머지가 어긋나도 모른다
        assertThat(INITIAL_STOCK + deltaSum).isEqualTo(finalStock);
    }

    //======================== 헬퍼 메소드 ===========================//

    /**
     * 동시 실행 결과
     * - success: 출고 성공
     * - insufficient: 재고 부족으로 거부됨. 예상된 실패다
     * - unexpected: 그 외 실패. 락 타임아웃·데드락·권한 오류가 여기 들어온다.
     *   둘을 섞으면 락이 깨졌을 때도 테스트가 통과한다
     */
    private record ConcurrentResult(int success, int insufficient, List<Throwable> unexpected) {}

    /** 테스트 대상 동작 — 창고 소유자로 DEDUCT_PER_THREAD 만큼 출고한다 */
    private void outbound() {
        inventoryService.adjustOut(ownerId, warehouseId, itemId,
                new OutboundRequest(DEDUCT_PER_THREAD, "동시성 테스트"));
    }

    /** DB에서 최종 재고를 다시 읽는다 */
    private int currentStock() {
        return inventoryRepository.findById(inventoryId)
                .map(Inventory::getQuantity).orElseThrow();
    }

    /**
     * task를 threadCount개 스레드에서 동시에 실행하고 결과를 집계한다.
     * - 래치로 전원을 대기시켰다가 한 번에 출발시킨다. 없으면 스레드가 순차 실행되어 경합이 생기지 않는다
     */
    private ConcurrentResult runConcurrently(int threadCount, Runnable task) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);

        AtomicInteger success = new AtomicInteger();
        AtomicInteger insufficient = new AtomicInteger();
        List<Throwable> unexpected = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                ready.countDown(); // try 밖에 둔다. 예외가 나도 메인이 무한 대기하지 않도록
                try {
                    start.await();
                    task.run();
                    success.incrementAndGet();
                } catch (ImsException e) {
                    if (e.getErrorCode() == ErrorCode.INSUFFICIENT_STOCK) {
                        insufficient.incrementAndGet(); // 예상된 실패 — 남은 재고보다 많이 빼려 한 경우
                    } else {
                        unexpected.add(e); // 예상 밖 — 권한 오류, 락 타임아웃 등
                    }
                } catch (Throwable t) {
                    unexpected.add(t); // Error까지 받는다. 워커 스레드에서 나면 조용히 사라진다
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        done.await();
        executor.shutdown();

        return new ConcurrentResult(success.get(), insufficient.get(), unexpected);
    }
}
