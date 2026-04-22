package com.ims.warehouse.service;

import com.ims.global.exception.ImsException;
import com.ims.partnership.service.PartnershipService;
import com.ims.user.entity.User;
import com.ims.user.repository.UserRepository;
import com.ims.warehouse.dto.request.ShareRequest;
import com.ims.warehouse.dto.response.WarehouseShareResponse;
import com.ims.warehouse.entity.Warehouse;
import com.ims.warehouse.entity.WarehouseShare;
import com.ims.warehouse.entity.WarehouseShare.SharePermission;
import com.ims.warehouse.repository.WarehouseRepository;
import com.ims.warehouse.repository.WarehouseShareRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class WarehouseShareServiceTest {

    @InjectMocks
    private WarehouseShareService warehouseShareService;

    @Mock
    private WarehouseShareRepository warehouseShareRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PartnershipService partnershipService;

    private User owner;
    private User target;
    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(1L).email("owner@test.com").password("encoded")
                .companyName("본사").companyCode("1000000001").build();

        target = User.builder()
                .id(2L).email("target@test.com").password("encoded")
                .companyName("하청").companyCode("2000000001").build();

        warehouse = Warehouse.builder()
                .id(1L).owner(owner).name("서울 창고").build();
    }

    @Test
    @DisplayName("창고 공유 성공 - 신규 생성")
    void share_success_new() {
        given(warehouseRepository.findById(1L)).willReturn(Optional.of(warehouse));
        given(userRepository.findByCompanyCode("2000000001")).willReturn(Optional.of(target));
        given(partnershipService.isPartner(1L, 2L)).willReturn(true);
        given(warehouseShareRepository.findByWarehouseIdAndSharedWithId(1L, 2L)).willReturn(Optional.empty());
        given(warehouseShareRepository.save(any())).willAnswer(i -> i.getArgument(0));

        ShareRequest request = new ShareRequest("2000000001", SharePermission.VIEW);
        WarehouseShareResponse response = warehouseShareService.share(1L, 1L, request);

        assertThat(response.permission()).isEqualTo("VIEW");
        then(warehouseShareRepository).should().save(any(WarehouseShare.class));
    }

    @Test
    @DisplayName("창고 공유 성공 - 권한 업데이트")
    void share_success_update() {
        WarehouseShare existing = WarehouseShare.builder()
                .id(1L).warehouse(warehouse).sharedWith(target).permission(SharePermission.VIEW).build();
        given(warehouseRepository.findById(1L)).willReturn(Optional.of(warehouse));
        given(userRepository.findByCompanyCode("2000000001")).willReturn(Optional.of(target));
        given(partnershipService.isPartner(1L, 2L)).willReturn(true);
        given(warehouseShareRepository.findByWarehouseIdAndSharedWithId(1L, 2L)).willReturn(Optional.of(existing));
        given(warehouseShareRepository.save(any())).willReturn(existing);

        ShareRequest request = new ShareRequest("2000000001", SharePermission.FULL);
        warehouseShareService.share(1L, 1L, request);

        assertThat(existing.getPermission()).isEqualTo(SharePermission.FULL);
    }

    @Test
    @DisplayName("창고 공유 실패 - 소유자 아님")
    void share_notOwner() {
        given(warehouseRepository.findById(1L)).willReturn(Optional.of(warehouse));

        assertThatThrownBy(() -> warehouseShareService.share(2L, 1L, new ShareRequest("2000000001", SharePermission.VIEW)))
                .isInstanceOf(ImsException.class);
    }

    @Test
    @DisplayName("창고 공유 실패 - Partnership 관계 없음")
    void share_notPartner() {
        given(warehouseRepository.findById(1L)).willReturn(Optional.of(warehouse));
        given(userRepository.findByCompanyCode("2000000001")).willReturn(Optional.of(target));
        given(partnershipService.isPartner(1L, 2L)).willReturn(false);

        assertThatThrownBy(() -> warehouseShareService.share(1L, 1L, new ShareRequest("2000000001", SharePermission.VIEW)))
                .isInstanceOf(ImsException.class);
    }

    @Test
    @DisplayName("공유 회수 성공")
    void revoke_success() {
        WarehouseShare share = WarehouseShare.builder()
                .id(1L).warehouse(warehouse).sharedWith(target).permission(SharePermission.VIEW).build();
        given(warehouseRepository.findById(1L)).willReturn(Optional.of(warehouse));
        given(userRepository.findByCompanyCode("2000000001")).willReturn(Optional.of(target));
        given(warehouseShareRepository.findByWarehouseIdAndSharedWithId(1L, 2L)).willReturn(Optional.of(share));

        warehouseShareService.revoke(1L, 1L, "2000000001");

        then(warehouseShareRepository).should().delete(share);
    }

    @Test
    @DisplayName("공유받은 창고 목록 조회 성공")
    void getSharedWarehouses_success() {
        WarehouseShare share = WarehouseShare.builder()
                .id(1L).warehouse(warehouse).sharedWith(target).permission(SharePermission.VIEW).build();
        given(warehouseShareRepository.findAllBySharedWithId(2L)).willReturn(List.of(share));

        List<WarehouseShareResponse> result = warehouseShareService.getSharedWarehouses(2L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).warehouseName()).isEqualTo("서울 창고");
    }

    @Test
    @DisplayName("공유 회수 실패 - 소유자 아님")
    void revoke_notOwner() {
        given(warehouseRepository.findById(1L)).willReturn(Optional.of(warehouse));

        assertThatThrownBy(() -> warehouseShareService.revoke(2L, 1L, "2000000001"))
                .isInstanceOf(ImsException.class);
    }

    @Test
    @DisplayName("공유 회수 실패 - 공유 없음")
    void revoke_shareNotFound() {
        given(warehouseRepository.findById(1L)).willReturn(Optional.of(warehouse));
        given(userRepository.findByCompanyCode("2000000001")).willReturn(Optional.of(target));
        given(warehouseShareRepository.findByWarehouseIdAndSharedWithId(1L, 2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseShareService.revoke(1L, 1L, "2000000001"))
                .isInstanceOf(ImsException.class);
    }

    @Test
    @DisplayName("FULL 권한 검증 성공 - 소유자")
    void checkFullAccess_owner() {
        given(warehouseRepository.findById(1L)).willReturn(Optional.of(warehouse));

        assertThatNoException().isThrownBy(() -> warehouseShareService.checkFullAccess(1L, 1L));
    }

    @Test
    @DisplayName("FULL 권한 검증 실패 - VIEW 권한만 있음")
    void checkFullAccess_viewOnly() {
        WarehouseShare share = WarehouseShare.builder()
                .id(1L).warehouse(warehouse).sharedWith(target).permission(SharePermission.VIEW).build();
        given(warehouseRepository.findById(1L)).willReturn(Optional.of(warehouse));
        given(warehouseShareRepository.findByWarehouseIdAndSharedWithId(1L, 2L)).willReturn(Optional.of(share));

        assertThatThrownBy(() -> warehouseShareService.checkFullAccess(2L, 1L))
                .isInstanceOf(ImsException.class);
    }

    @Test
    @DisplayName("FULL 권한 검증 실패 - 공유 없음")
    void checkFullAccess_noShare() {
        given(warehouseRepository.findById(1L)).willReturn(Optional.of(warehouse));
        given(warehouseShareRepository.findByWarehouseIdAndSharedWithId(1L, 2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseShareService.checkFullAccess(2L, 1L))
                .isInstanceOf(ImsException.class);
    }

    @Test
    @DisplayName("VIEW 권한 검증 성공 - 공유받은 유저")
    void checkViewAccess_shared() {
        WarehouseShare share = WarehouseShare.builder()
                .id(1L).warehouse(warehouse).sharedWith(target).permission(SharePermission.VIEW).build();
        given(warehouseRepository.findById(1L)).willReturn(Optional.of(warehouse));
        given(warehouseShareRepository.findByWarehouseIdAndSharedWithId(1L, 2L)).willReturn(Optional.of(share));

        assertThatNoException().isThrownBy(() -> warehouseShareService.checkViewAccess(2L, 1L));
    }

    @Test
    @DisplayName("VIEW 권한 검증 실패 - 공유 없음")
    void checkViewAccess_noShare() {
        given(warehouseRepository.findById(1L)).willReturn(Optional.of(warehouse));
        given(warehouseShareRepository.findByWarehouseIdAndSharedWithId(1L, 2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseShareService.checkViewAccess(2L, 1L))
                .isInstanceOf(ImsException.class);
    }
}
