package com.ims.inventory.repository;

import com.ims.global.config.JpaAuditingConfig;
import com.ims.inventory.entity.Inventory;
import com.ims.item.entity.Item;
import com.ims.item.entity.ItemType;
import com.ims.item.repository.ItemRepository;
import com.ims.user.entity.User;
import com.ims.user.repository.UserRepository;
import com.ims.warehouse.entity.Warehouse;
import com.ims.warehouse.repository.WarehouseRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.*;

/**
 * 재고 목록 조회의 쿼리 수 검증
 * - InventoryResponse가 품목 코드·이름·타입을 읽으므로 item을 함께 로딩해야 한다
 * - @EntityGraph 없이는 행마다 Item SELECT가 추가로 나간다
 * - Specification + Pageable 조합에서 count 쿼리가 어긋나지 않는지도 함께 확인한다
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(JpaAuditingConfig.class)
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class InventoryRepositoryTest {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        User owner = userRepository.save(User.builder()
                .email("owner@ims.dev").password("encoded")
                .companyName("소유자회사").companyCode("1000000001").build());

        warehouse = warehouseRepository.save(Warehouse.builder()
                .owner(owner).name("서울창고").location("서울").build());

        // 재고 5건 — N+1이 발생하면 쿼리가 행 수만큼 늘어난다
        for (int i = 1; i <= 5; i++) {
            Item item = itemRepository.save(Item.builder()
                    .owner(owner).itemCode("P%03d".formatted(i))
                    .name("부품" + i).type(ItemType.PART).build());
            inventoryRepository.save(Inventory.builder()
                    .warehouse(warehouse).item(item).quantity(10).safetyStock(1).build());
        }
        entityManager.flush();
        entityManager.clear();
    }

    private Statistics statistics() {
        return entityManager.unwrap(Session.class).getSessionFactory().getStatistics();
    }

    @Test
    @DisplayName("재고 목록 조회 시 item을 함께 로딩해 행 수와 무관하게 쿼리 수가 고정된다")
    void findAll_withSpecification_doesNotTriggerNPlusOne() {
        Specification<Inventory> spec = (root, query, cb) ->
                cb.equal(root.get("warehouse").get("id"), warehouse.getId());

        Statistics stats = statistics();
        stats.clear();

        Page<Inventory> page = inventoryRepository.findAll(spec, PageRequest.of(0, 10));

        // 연관 엔티티 접근 — EntityGraph가 없으면 여기서 행마다 SELECT가 나간다
        page.getContent().forEach(inv -> {
            inv.getItem().getItemCode();
            inv.getItem().getName();
            inv.getItem().getType();
        });

        assertThat(page.getContent()).hasSize(5);
        // 목록 조회 1회로 끝나야 한다.
        // 결과가 페이지 크기보다 작으면 Spring Data가 count 쿼리를 생략하므로 1회다.
        // EntityGraph가 없으면 행마다 Item SELECT가 붙어 1 + 5 = 6회가 된다.
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("EntityGraph를 적용해도 페이지네이션 totalElements는 정확하다")
    void findAll_withSpecification_countIsAccurate() {
        Specification<Inventory> spec = (root, query, cb) ->
                cb.equal(root.get("warehouse").get("id"), warehouse.getId());

        Page<Inventory> firstPage = inventoryRepository.findAll(spec, PageRequest.of(0, 2));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(5);
        assertThat(firstPage.getTotalPages()).isEqualTo(3);
    }
}
