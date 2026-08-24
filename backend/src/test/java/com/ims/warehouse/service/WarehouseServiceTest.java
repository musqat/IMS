package com.ims.warehouse.service;

import com.ims.global.exception.ImsException;
import com.ims.global.exception.ErrorCode;
import com.ims.global.support.DomainValidator;
import com.ims.user.entity.User;
import com.ims.user.repository.UserRepository;
import com.ims.warehouse.dto.request.WarehouseCreateRequest;
import com.ims.warehouse.dto.response.WarehouseResponse;
import com.ims.warehouse.entity.Warehouse;
import com.ims.inventory.repository.InventoryRepository;
import com.ims.production.repository.ProductionRepository;
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
class WarehouseServiceTest {

    @InjectMocks
    private WarehouseService warehouseService;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DomainValidator domainValidator;

    @Mock
    private WarehouseShareService warehouseShareService;

    @Mock
    private WarehouseShareRepository warehouseShareRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProductionRepository productionRepository;

    private User user;
    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@test.com")
                .password("encoded")
                .companyName("테스트회사")
                .companyCode("1000000001")
                .build();

        warehouse = Warehouse.builder()
                .id(1L)
                .owner(user)
                .name("서울 창고")
                .location("서울시 강남구")
                .build();
    }

    @Test
    @DisplayName("창고 생성 성공")
    void createWarehouse_success() {
        // given
        WarehouseCreateRequest request = new WarehouseCreateRequest("서울 창고", "서울시 강남구");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(warehouseRepository.save(any())).willReturn(warehouse);

        // when
        WarehouseResponse response = warehouseService.createWarehouse(1L, request);

        // then
        assertThat(response.name()).isEqualTo("서울 창고");
        then(warehouseRepository).should().save(any(Warehouse.class));
    }

    @Test
    @DisplayName("창고 생성 실패 - 존재하지 않는 User")
    void createWarehouse_userNotFound() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> warehouseService.createWarehouse(1L, new WarehouseCreateRequest("창고", null)))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("창고 전체 조회 성공")
    void getWarehouses_success() {
        // given
        given(warehouseRepository.findAllByOwnerIdAndActiveTrue(1L)).willReturn(List.of(warehouse));

        // when
        List<WarehouseResponse> result = warehouseService.getWarehouses(1L);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("서울 창고");
    }

    @Test
    @DisplayName("창고 단건 조회 성공")
    void getWarehouse_success() {
        // given
        given(warehouseShareService.checkViewAccess(1L, 1L)).willReturn(warehouse);

        // when
        WarehouseResponse response = warehouseService.getWarehouse(1L, 1L);

        // then
        assertThat(response.name()).isEqualTo("서울 창고");
    }

    @Test
    @DisplayName("창고 단건 조회 실패 - 소유자도 공유 대상도 아님")
    void getWarehouse_noAccess() {
        // given
        given(warehouseShareService.checkViewAccess(2L, 1L))
                .willThrow(new ImsException(ErrorCode.WAREHOUSE_ACCESS_DENIED));

        // when & then
        assertThatThrownBy(() -> warehouseService.getWarehouse(2L, 1L))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WAREHOUSE_ACCESS_DENIED);
    }

    @Test
    @DisplayName("창고 삭제 성공")
    void deleteWarehouse_success() {
        // given
        given(domainValidator.getOwnedWarehouse(1L, 1L)).willReturn(warehouse);

        // when
        warehouseService.deleteWarehouse(1L, 1L);

        // then
        then(warehouseRepository).should().delete(warehouse);
    }

    @Test
    @DisplayName("창고 삭제 실패 - 소유자 아님")
    void deleteWarehouse_notOwner() {
        // given
        given(domainValidator.getOwnedWarehouse(2L, 1L)).willThrow(new ImsException(ErrorCode.WAREHOUSE_NOT_OWNED));

        // when & then
        assertThatThrownBy(() -> warehouseService.deleteWarehouse(2L, 1L))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WAREHOUSE_NOT_OWNED);
    }

    @Test
    @DisplayName("창고 단건 조회 - 공유받은 사용자도 조회할 수 있다")
    void getWarehouse_sharedUser_canRead() {
        // given: 999L은 소유자가 아니지만 창고를 공유받았다
        Long sharedUserId = 999L;
        given(warehouseShareService.checkViewAccess(sharedUserId, warehouse.getId()))
                .willReturn(warehouse);

        // when
        WarehouseResponse result = warehouseService.getWarehouse(sharedUserId, warehouse.getId());

        // then: 소유자 전용 검증을 쓰면 공유 창고 상세 화면이 이름조차 못 받는다
        then(warehouseShareService).should().checkViewAccess(sharedUserId, warehouse.getId());
        then(domainValidator).should(never()).getOwnedWarehouse(any(), any());
        assertThat(result.id()).isEqualTo(warehouse.getId());
    }

    // ===================== 창고 삭제 참조 검증 =====================
    // 재고·생산 기록이 남은 창고를 지우면 FK 위반이 나고, 그 예외가
    // DUPLICATE_RESOURCE("이미 존재하는 리소스입니다")로 매핑돼 엉뚱한 메시지가 나갔다.
    // 삭제 전에 참조를 직접 확인해 무엇이 막는지 알려준다.

    @Test
    @DisplayName("창고 삭제 실패 - 재고가 남아 있음")
    void deleteWarehouse_hasInventory() {
        // given
        given(domainValidator.getOwnedWarehouse(1L, 1L)).willReturn(warehouse);
        given(inventoryRepository.existsByWarehouseId(1L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> warehouseService.deleteWarehouse(1L, 1L))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WAREHOUSE_HAS_INVENTORY);
        then(warehouseRepository).should(never()).delete(any());
    }

    @Test
    @DisplayName("창고 삭제 실패 - 생산 기록이 있음")
    void deleteWarehouse_hasProduction() {
        // given
        given(domainValidator.getOwnedWarehouse(1L, 1L)).willReturn(warehouse);
        given(inventoryRepository.existsByWarehouseId(1L)).willReturn(false);
        given(productionRepository.existsByWarehouseId(1L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> warehouseService.deleteWarehouse(1L, 1L))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WAREHOUSE_HAS_PRODUCTION);
        then(warehouseRepository).should(never()).delete(any());
    }

    @Test
    @DisplayName("창고 삭제 - 공유 설정은 함께 삭제한다")
    void deleteWarehouse_removesShares() {
        // given
        // 창고가 사라지면 그 창고의 공유도 의미가 없다. 재고·생산 기록과 달리 남길 이유가 없다
        given(domainValidator.getOwnedWarehouse(1L, 1L)).willReturn(warehouse);
        given(inventoryRepository.existsByWarehouseId(1L)).willReturn(false);
        given(productionRepository.existsByWarehouseId(1L)).willReturn(false);

        // when
        warehouseService.deleteWarehouse(1L, 1L);

        // then
        then(warehouseShareRepository).should().deleteAllByWarehouseId(1L);
        then(warehouseRepository).should().delete(warehouse);
    }

    // ===================== 소프트 삭제 =====================
    // 재고·생산 기록이 있으면 물리 삭제를 할 수 없다. 분석의 원본이기 때문이다.
    // 대신 창고를 닫는다. 목록과 쓰기에서 빠지고 과거 이력 조회는 유지된다.

    @Test
    @DisplayName("창고 비활성화 - active를 false로 바꾸고 삭제하지 않는다")
    void deactivateWarehouse_keepsRecord() {
        // given
        given(domainValidator.getOwnedWarehouse(1L, 1L)).willReturn(warehouse);

        // when
        warehouseService.deactivateWarehouse(1L, 1L);

        // then
        assertThat(warehouse.isActive()).isFalse();
        then(warehouseRepository).should(never()).delete(any());
    }

    @Test
    @DisplayName("창고 비활성화 실패 - 소유자 아님")
    void deactivateWarehouse_notOwner() {
        // given
        given(domainValidator.getOwnedWarehouse(2L, 1L))
                .willThrow(new ImsException(ErrorCode.WAREHOUSE_NOT_OWNED));

        // when & then
        assertThatThrownBy(() -> warehouseService.deactivateWarehouse(2L, 1L))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WAREHOUSE_NOT_OWNED);
    }

    @Test
    @DisplayName("창고 재활성화 - 비활성 창고를 다시 활성화한다")
    void activateWarehouse_reopens() {
        // given
        warehouse.deactivate();
        given(domainValidator.getOwnedWarehouse(1L, 1L)).willReturn(warehouse);

        // when
        warehouseService.activateWarehouse(1L, 1L);

        // then
        assertThat(warehouse.isActive()).isTrue();
    }

    @Test
    @DisplayName("창고 목록 조회 - 비활성 창고는 제외한다")
    void getWarehouses_excludesInactive() {
        // given
        // 비활성 창고가 목록에 남으면 닫은 의미가 없다
        given(warehouseRepository.findAllByOwnerIdAndActiveTrue(1L)).willReturn(List.of(warehouse));

        // when
        List<WarehouseResponse> result = warehouseService.getWarehouses(1L);

        // then
        assertThat(result).hasSize(1);
        then(warehouseRepository).should(never()).findAllByOwnerId(any());
    }
}
