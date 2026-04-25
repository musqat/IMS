package com.ims.item.repository;

import com.ims.global.config.JpaAuditingConfig;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(JpaAuditingConfig.class)
class ItemRepositoryTest {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(User.builder()
                .email("owner@test.com").password("pw").companyName("테스트회사").companyCode("1000000001").build());
    }

    @Test
    @DisplayName("소유자 기준 품목 전체 조회")
    void findAllByOwnerId_success() {
        itemRepository.save(Item.builder().owner(owner).itemCode("A-001").name("완성품A").type(ItemType.FINISHED).build());
        itemRepository.save(Item.builder().owner(owner).itemCode("B-001").name("부품B").type(ItemType.PART).build());

        List<Item> result = itemRepository.findAllByOwnerId(owner.getId());

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("itemCode 중복 여부 - true")
    void existsByOwnerIdAndItemCode_true() {
        itemRepository.save(Item.builder().owner(owner).itemCode("A-001").name("완성품A").type(ItemType.FINISHED).build());

        assertThat(itemRepository.existsByOwnerIdAndItemCode(owner.getId(), "A-001")).isTrue();
    }

    @Test
    @DisplayName("itemCode 중복 여부 - false")
    void existsByOwnerIdAndItemCode_false() {
        assertThat(itemRepository.existsByOwnerIdAndItemCode(owner.getId(), "A-001")).isFalse();
    }

    @Test
    @DisplayName("owner + itemCode로 단건 조회 성공")
    void findByOwnerIdAndItemCode_success() {
        itemRepository.save(Item.builder().owner(owner).itemCode("A-001").name("완성품A").type(ItemType.FINISHED).build());

        Optional<Item> result = itemRepository.findByOwnerIdAndItemCode(owner.getId(), "A-001");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("완성품A");
    }

    @Test
    @DisplayName("owner + itemCode로 단건 조회 실패 - 다른 owner의 동일 코드는 조회되지 않음")
    void findByOwnerIdAndItemCode_differentOwner() {
        User other = userRepository.save(User.builder()
                .email("other@test.com").password("pw").companyName("다른회사").companyCode("9999999999").build());
        itemRepository.save(Item.builder().owner(other).itemCode("A-001").name("타사품목").type(ItemType.PART).build());

        Optional<Item> result = itemRepository.findByOwnerIdAndItemCode(owner.getId(), "A-001");

        assertThat(result).isEmpty();
    }
}
