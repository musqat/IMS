package com.ims.warehouse.repository;

import com.ims.global.config.JpaAuditingConfig;
import com.ims.user.entity.User;
import com.ims.user.repository.UserRepository;
import com.ims.warehouse.entity.Warehouse;
import com.ims.warehouse.entity.WarehouseShare;
import com.ims.warehouse.entity.WarehouseShare.SharePermission;
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
class WarehouseShareRepositoryTest {

    @Autowired
    private WarehouseShareRepository warehouseShareRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;
    private User guest;
    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(User.builder()
                .email("owner@test.com").password("pw").companyName("본사").companyCode("1000000001").build());
        guest = userRepository.save(User.builder()
                .email("guest@test.com").password("pw").companyName("하청").companyCode("2000000002").build());
        warehouse = warehouseRepository.save(
                Warehouse.builder().owner(owner).name("서울 창고").location("서울").build());
    }

    @Test
    @DisplayName("공유받은 창고 목록 조회")
    void findAllBySharedWithId_success() {
        warehouseShareRepository.save(WarehouseShare.builder()
                .warehouse(warehouse).sharedWith(guest).permission(SharePermission.VIEW).build());

        List<WarehouseShare> result = warehouseShareRepository.findAllBySharedWithId(guest.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getWarehouse().getId()).isEqualTo(warehouse.getId());
    }

    @Test
    @DisplayName("특정 창고 + 특정 유저의 공유 권한 조회 성공")
    void findByWarehouseIdAndSharedWithId_success() {
        warehouseShareRepository.save(WarehouseShare.builder()
                .warehouse(warehouse).sharedWith(guest).permission(SharePermission.FULL).build());

        Optional<WarehouseShare> result = warehouseShareRepository
                .findByWarehouseIdAndSharedWithId(warehouse.getId(), guest.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getPermission()).isEqualTo(SharePermission.FULL);
    }

    @Test
    @DisplayName("특정 창고 + 특정 유저의 공유 권한 조회 실패 - 없는 경우")
    void findByWarehouseIdAndSharedWithId_notFound() {
        Optional<WarehouseShare> result = warehouseShareRepository
                .findByWarehouseIdAndSharedWithId(warehouse.getId(), guest.getId());

        assertThat(result).isEmpty();
    }
}
