package com.ims.warehouse.repository;

import com.ims.global.config.JpaAuditingConfig;
import com.ims.user.entity.User;
import com.ims.user.repository.UserRepository;
import com.ims.warehouse.entity.Warehouse;
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
class WarehouseRepositoryTest {

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(User.builder()
                .email("owner@test.com").password("pw").companyName("테스트회사").companyCode("1000000001").build());
    }

    @Test
    @DisplayName("소유자 기준 창고 전체 조회")
    void findAllByOwnerId_success() {
        warehouseRepository.save(Warehouse.builder().owner(owner).name("창고A").location("서울").build());
        warehouseRepository.save(Warehouse.builder().owner(owner).name("창고B").location("부산").build());

        List<Warehouse> result = warehouseRepository.findAllByOwnerId(owner.getId());

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("소유자 기준 창고 조회 - 다른 소유자 결과 미포함")
    void findAllByOwnerId_excludesOtherOwner() {
        User other = userRepository.save(User.builder()
                .email("other@test.com").password("pw").companyName("다른회사").companyCode("9999999999").build());
        warehouseRepository.save(Warehouse.builder().owner(owner).name("내 창고").location("서울").build());
        warehouseRepository.save(Warehouse.builder().owner(other).name("남의 창고").location("부산").build());

        List<Warehouse> result = warehouseRepository.findAllByOwnerId(owner.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("내 창고");
    }
}
