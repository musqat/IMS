package com.ims.warehouse.service;

import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.global.support.DomainValidator;
import com.ims.user.entity.User;
import com.ims.user.repository.UserRepository;
import com.ims.warehouse.dto.request.WarehouseCreateRequest;
import com.ims.warehouse.dto.response.WarehouseResponse;
import com.ims.warehouse.entity.Warehouse;
import com.ims.warehouse.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;
    private final DomainValidator domainValidator;
    private final WarehouseShareService warehouseShareService;

    /**
     * 창고 생성
     * - User 조회
     * - 창고 저장 후 응답 반환
     */
    @Transactional
    public WarehouseResponse createWarehouse(Long userId, WarehouseCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ImsException(ErrorCode.USER_NOT_FOUND));

        Warehouse warehouse = Warehouse.builder()
                .owner(user)
                .name(request.name())
                .location(request.location())
                .build();

        return WarehouseResponse.from(warehouseRepository.save(warehouse));
    }

    /**
     * 창고 전체 조회
     * - 해당 User 소유 창고 전체 반환
     */
    public List<WarehouseResponse> getWarehouses(Long userId) {
        return warehouseRepository.findAllByOwnerId(userId).stream()
                .map(WarehouseResponse::from)
                .toList();
    }

    /**
     * 창고 단건 조회
     * - 조회 권한 검증 후 반환 (소유자 또는 공유받은 사용자)
     * - 소유자 전용으로 두면 공유 창고 상세 화면이 창고 이름조차 받지 못한다
     */
    public WarehouseResponse getWarehouse(Long userId, Long warehouseId) {
        Warehouse warehouse = warehouseShareService.checkViewAccess(userId, warehouseId);
        return WarehouseResponse.from(warehouse);
    }

    /**
     * 창고 삭제
     * - 소유자 검증 후 삭제
     */
    @Transactional
    public void deleteWarehouse(Long userId, Long warehouseId) {
        Warehouse warehouse = domainValidator.getOwnedWarehouse(userId, warehouseId);
        warehouseRepository.delete(warehouse);
    }
}
