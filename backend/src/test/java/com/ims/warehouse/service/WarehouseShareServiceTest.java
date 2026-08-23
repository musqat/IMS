package com.ims.warehouse.service;

import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.global.support.DomainValidator;
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
    private DomainValidator domainValidator;

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
        given(domainValidator.getOwnedWarehouse(1L, 1L)).willReturn(warehouse);
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
        given(domainValidator.getOwnedWarehouse(1L, 1L)).willReturn(warehouse);
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
        given(domainValidator.getOwnedWarehouse(2L, 1L)).willThrow(new ImsException(ErrorCode.WAREHOUSE_NOT_OWNED));

        assertThatThrownBy(() -> warehouseShareService.share(2L, 1L, new ShareRequest("2000000001", SharePermission.VIEW)))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WAREHOUSE_NOT_OWNED);
    }

    @Test
    @DisplayName("창고 공유 실패 - Partnership 관계 없음")
    void share_notPartner() {
        given(domainValidator.getOwnedWarehouse(1L, 1L)).willReturn(warehouse);
        given(userRepository.findByCompanyCode("2000000001")).willReturn(Optional.of(target));
        given(partnershipService.isPartner(1L, 2L)).willReturn(false);

        assertThatThrownBy(() -> warehouseShareService.share(1L, 1L, new ShareRequest("2000000001", SharePermission.VIEW)))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_PARTNER);
    }

    @Test
    @DisplayName("공유 회수 성공")
    void revoke_success() {
        WarehouseShare share = WarehouseShare.builder()
                .id(1L).warehouse(warehouse).sharedWith(target).permission(SharePermission.VIEW).build();
        given(domainValidator.getOwnedWarehouse(1L, 1L)).willReturn(warehouse);
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
        given(domainValidator.getOwnedWarehouse(2L, 1L)).willThrow(new ImsException(ErrorCode.WAREHOUSE_NOT_OWNED));

        assertThatThrownBy(() -> warehouseShareService.revoke(2L, 1L, "2000000001"))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WAREHOUSE_NOT_OWNED);
    }

    @Test
    @DisplayName("공유 회수 실패 - 공유 없음")
    void revoke_shareNotFound() {
        given(domainValidator.getOwnedWarehouse(1L, 1L)).willReturn(warehouse);
        given(userRepository.findByCompanyCode("2000000001")).willReturn(Optional.of(target));
        given(warehouseShareRepository.findByWarehouseIdAndSharedWithId(1L, 2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseShareService.revoke(1L, 1L, "2000000001"))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WAREHOUSE_SHARE_NOT_FOUND);
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
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WAREHOUSE_ACCESS_DENIED);
    }

    @Test
    @DisplayName("FULL 권한 검증 실패 - 공유 없음")
    void checkFullAccess_noShare() {
        given(warehouseRepository.findById(1L)).willReturn(Optional.of(warehouse));
        given(warehouseShareRepository.findByWarehouseIdAndSharedWithId(1L, 2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseShareService.checkFullAccess(2L, 1L))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WAREHOUSE_ACCESS_DENIED);
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
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WAREHOUSE_ACCESS_DENIED);
    }

    @Test
    @DisplayName("창고 공유 실패 - companyCode에 해당하는 User 없음")
    void share_userNotFound() {
        // given
        given(domainValidator.getOwnedWarehouse(1L, 1L)).willReturn(warehouse);
        given(userRepository.findByCompanyCode("9999999999")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> warehouseShareService.share(1L, 1L, new ShareRequest("9999999999", SharePermission.VIEW)))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("VIEW 권한 검증 성공 - FULL 권한 공유자도 조회 통과 (VIEW 이상이면 허용)")
    void checkViewAccess_fullPermissionUser_passes() {
        // given: target은 FULL 권한 — VIEW 이상이면 조회 통과
        WarehouseShare fullShare = WarehouseShare.builder()
                .id(1L).warehouse(warehouse).sharedWith(target).permission(SharePermission.FULL).build();
        given(warehouseRepository.findById(1L)).willReturn(Optional.of(warehouse));
        given(warehouseShareRepository.findByWarehouseIdAndSharedWithId(1L, 2L)).willReturn(Optional.of(fullShare));

        // when & then: FULL 권한도 checkViewAccess 통과
        assertThatNoException().isThrownBy(() -> warehouseShareService.checkViewAccess(2L, 1L));
    }

    @Test
    @DisplayName("FULL 권한 검증 성공 - FULL 권한 공유자 통과")
    void checkFullAccess_fullPermissionUser_passes() {
        // given: target은 FULL 권한으로 공유받은 User
        WarehouseShare fullShare = WarehouseShare.builder()
                .id(1L).warehouse(warehouse).sharedWith(target).permission(SharePermission.FULL).build();
        given(warehouseRepository.findById(1L)).willReturn(Optional.of(warehouse));
        given(warehouseShareRepository.findByWarehouseIdAndSharedWithId(1L, 2L)).willReturn(Optional.of(fullShare));

        // when & then: FULL 권한자는 예외 없이 통과
        assertThatNoException().isThrownBy(() -> warehouseShareService.checkFullAccess(2L, 1L));
    }

    // ===================== 비활성 창고 =====================
    // 닫은 창고는 목록과 쓰기에서 빠진다. 다만 과거 이력은 계속 볼 수 있어야 하므로
    // 조회 권한(checkViewAccess)은 통과시킨다.

    @Test
    @DisplayName("쓰기 권한 - 비활성 창고는 소유자도 거부된다")
    void checkFullAccess_inactiveWarehouse_denied() {
        warehouse.deactivate();
        given(warehouseRepository.findById(warehouse.getId())).willReturn(Optional.of(warehouse));

        assertThatThrownBy(() -> warehouseShareService.checkFullAccess(owner.getId(), warehouse.getId()))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WAREHOUSE_INACTIVE);
    }

    @Test
    @DisplayName("조회 권한 - 비활성 창고도 과거 이력 조회는 가능하다")
    void checkViewAccess_inactiveWarehouse_allowed() {
        // 닫았다고 지난 데이터가 안 보이면 소프트 삭제의 의미가 없다
        warehouse.deactivate();
        given(warehouseRepository.findById(warehouse.getId())).willReturn(Optional.of(warehouse));

        Warehouse result = warehouseShareService.checkViewAccess(owner.getId(), warehouse.getId());

        assertThat(result.isActive()).isFalse();
    }

    @Test
    @DisplayName("접근 가능 창고 ID - 비활성 창고는 제외한다")
    void getAccessibleWarehouseIds_excludesInactive() {
        given(warehouseRepository.findAllByOwnerIdAndActiveTrue(owner.getId())).willReturn(List.of(warehouse));
        given(warehouseShareRepository.findAllBySharedWithId(owner.getId())).willReturn(List.of());

        List<Long> result = warehouseShareService.getAccessibleWarehouseIds(owner.getId());

        assertThat(result).containsExactly(warehouse.getId());
        then(warehouseRepository).should(never()).findAllByOwnerId(any());
    }

    @Test
    @DisplayName("공유받은 창고 목록 - 소유자가 닫은 창고는 제외한다")
    void getSharedWarehouses_excludesInactive() {
        // 남이 닫은 창고가 내 공유 목록에 남으면 클릭했을 때 쓰기가 막혀 혼란스럽다
        Warehouse closed = Warehouse.builder()
                .id(99L).owner(owner).name("닫은창고").build();
        closed.deactivate();

        WarehouseShare activeShare = WarehouseShare.builder()
                .id(1L).warehouse(warehouse).sharedWith(target)
                .permission(SharePermission.VIEW).build();
        WarehouseShare closedShare = WarehouseShare.builder()
                .id(2L).warehouse(closed).sharedWith(target)
                .permission(SharePermission.VIEW).build();

        given(warehouseShareRepository.findAllBySharedWithId(target.getId()))
                .willReturn(List.of(activeShare, closedShare));

        List<WarehouseShareResponse> result = warehouseShareService.getSharedWarehouses(target.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).warehouseId()).isEqualTo(warehouse.getId());
    }
}
