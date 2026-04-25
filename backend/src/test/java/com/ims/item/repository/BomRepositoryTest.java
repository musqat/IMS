package com.ims.item.repository;

import com.ims.global.config.JpaAuditingConfig;
import com.ims.item.entity.Bom;
import com.ims.item.entity.Item;
import com.ims.item.entity.ItemType;
import com.ims.user.entity.User;
import com.ims.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(JpaAuditingConfig.class)
class BomRepositoryTest {

    @Autowired
    private BomRepository bomRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    private Item itemA;  // FINISHED
    private Item itemB;  // SEMI
    private Item itemC;  // PART

    @BeforeEach
    void setUp() {
        User owner = userRepository.save(User.builder()
                .email("owner@test.com").password("pw").companyName("테스트회사").companyCode("1000000001").build());
        itemA = itemRepository.save(Item.builder().owner(owner).itemCode("A").name("완성품A").type(ItemType.FINISHED).build());
        itemB = itemRepository.save(Item.builder().owner(owner).itemCode("B").name("반제품B").type(ItemType.SEMI).build());
        itemC = itemRepository.save(Item.builder().owner(owner).itemCode("C").name("부품C").type(ItemType.PART).build());
    }

    @Test
    @DisplayName("parent 기준 직접 하위 BOM 목록 조회")
    void findAllByParentId_success() {
        bomRepository.save(Bom.builder().parent(itemA).child(itemB).quantity(1).build());
        bomRepository.save(Bom.builder().parent(itemA).child(itemC).quantity(3).build());

        List<Bom> result = bomRepository.findAllByParentId(itemA.getId());

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("parent + child 쌍 중복 여부 - true")
    void existsByParentIdAndChildId_true() {
        bomRepository.save(Bom.builder().parent(itemA).child(itemB).quantity(1).build());

        assertThat(bomRepository.existsByParentIdAndChildId(itemA.getId(), itemB.getId())).isTrue();
    }

    @Test
    @DisplayName("parent + child 쌍 중복 여부 - false")
    void existsByParentIdAndChildId_false() {
        assertThat(bomRepository.existsByParentIdAndChildId(itemA.getId(), itemB.getId())).isFalse();
    }

    @Test
    @DisplayName("parent 참조 존재 여부")
    void existsByParentId_true() {
        bomRepository.save(Bom.builder().parent(itemA).child(itemB).quantity(1).build());

        assertThat(bomRepository.existsByParentId(itemA.getId())).isTrue();
    }

    @Test
    @DisplayName("child 참조 존재 여부")
    void existsByChildId_true() {
        bomRepository.save(Bom.builder().parent(itemA).child(itemB).quantity(1).build());

        assertThat(bomRepository.existsByChildId(itemB.getId())).isTrue();
    }
}
