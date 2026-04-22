package com.ims.warehouse.service;

import com.ims.global.exception.ImsException;
import com.ims.user.entity.User;
import com.ims.user.repository.UserRepository;
import com.ims.warehouse.dto.request.WarehouseCreateRequest;
import com.ims.warehouse.dto.response.WarehouseResponse;
import com.ims.warehouse.entity.Warehouse;
import com.ims.warehouse.repository.WarehouseRepository;
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
        WarehouseCreateRequest request = new WarehouseCreateRequest("서울 창고", "서울시 강남구");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(warehouseRepository.save(any())).willReturn(warehouse);

        WarehouseResponse response = warehouseService.createWarehouse(1L, request);

        assertThat(response.name()).isEqualTo("서울 창고");
        then(warehouseRepository).should().save(any(Warehouse.class));
    }

    @Test
    @DisplayName("창고 생성 실패 - 존재하지 않는 User")
    void createWarehouse_userNotFound() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseService.createWarehouse(1L, new WarehouseCreateRequest("창고", null)))
                .isInstanceOf(ImsException.class);
    }

    @Test
    @DisplayName("창고 전체 조회 성공")
    void getWarehouses_success() {
        given(warehouseRepository.findAllByOwnerId(1L)).willReturn(List.of(warehouse));

        List<WarehouseResponse> result = warehouseService.getWarehouses(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("서울 창고");
    }

    @Test
    @DisplayName("창고 단건 조회 성공")
    void getWarehouse_success() {
        given(warehouseRepository.findById(1L)).willReturn(Optional.of(warehouse));

        WarehouseResponse response = warehouseService.getWarehouse(1L, 1L);

        assertThat(response.name()).isEqualTo("서울 창고");
    }

    @Test
    @DisplayName("창고 단건 조회 실패 - 다른 소유자")
    void getWarehouse_notOwner() {
        given(warehouseRepository.findById(1L)).willReturn(Optional.of(warehouse));

        assertThatThrownBy(() -> warehouseService.getWarehouse(2L, 1L))
                .isInstanceOf(ImsException.class);
    }

    @Test
    @DisplayName("창고 삭제 성공")
    void deleteWarehouse_success() {
        given(warehouseRepository.findById(1L)).willReturn(Optional.of(warehouse));

        warehouseService.deleteWarehouse(1L, 1L);

        then(warehouseRepository).should().delete(warehouse);
    }

    @Test
    @DisplayName("창고 삭제 실패 - 소유자 아님")
    void deleteWarehouse_notOwner() {
        given(warehouseRepository.findById(1L)).willReturn(Optional.of(warehouse));

        assertThatThrownBy(() -> warehouseService.deleteWarehouse(2L, 1L))
                .isInstanceOf(ImsException.class);
    }
}
