package com.ims.warehouse.service;

import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
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
     * - warehouseId로 조회 후 소유자 검증
     */
    public WarehouseResponse getWarehouse(Long userId, Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ImsException(ErrorCode.WAREHOUSE_NOT_FOUND));
        if (!warehouse.getOwner().getId().equals(userId)) {
            throw new ImsException(ErrorCode.FORBIDDEN);
        }
        return WarehouseResponse.from(warehouse);
    }

    /**
     * 창고 삭제
     * - 소유자 검증 후 삭제
     */
    @Transactional
    public void deleteWarehouse(Long userId, Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ImsException(ErrorCode.WAREHOUSE_NOT_FOUND));
        if (!warehouse.getOwner().getId().equals(userId)) {
            throw new ImsException(ErrorCode.FORBIDDEN);
        }
        warehouseRepository.delete(warehouse);
    }
}
