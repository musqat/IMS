package com.ims.warehouse.service;

import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
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
    private final WarehouseShareRepository warehouseShareRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductionRepository productionRepository;

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
        return warehouseRepository.findAllByOwnerIdAndActiveTrue(userId).stream()
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
     * - 재고와 생산 기록은 분석의 원본이라 창고와 함께 지우지 않는다
     * - 공유 설정은 창고에 종속된 정보라 함께 지운다
     */
    @Transactional
    public void deleteWarehouse(Long userId, Long warehouseId) {
        Warehouse warehouse = domainValidator.getOwnedWarehouse(userId, warehouseId);

        if (inventoryRepository.existsByWarehouseId(warehouseId)) {
            throw new ImsException(ErrorCode.WAREHOUSE_HAS_INVENTORY);
        }
        if (productionRepository.existsByWarehouseId(warehouseId)) {
            throw new ImsException(ErrorCode.WAREHOUSE_HAS_PRODUCTION);
        }

        warehouseShareRepository.deleteAllByWarehouseId(warehouseId);
        warehouseRepository.delete(warehouse);
    }

    /**
     * 비활성 창고 목록
     */
    public List<WarehouseResponse> getInactiveWarehouses(Long userId) {
        return warehouseRepository.findAllByOwnerIdAndActiveFalse(userId).stream()
                .map(WarehouseResponse::from)
                .toList();
    }

    /**
     * 창고 비활성화 (소프트 삭제)
     * - 재고나 생산 기록이 있으면 물리 삭제를 할 수 없다. 분석의 원본이기 때문
     * - 목록과 쓰기 경로에서 빠지고 과거 이력 조회는 유지된다
     * - 공유 설정도 그대로 둔다. 다시 활성화하면 이전 상태로 돌아간다
     */
    @Transactional
    public void deactivateWarehouse(Long userId, Long warehouseId) {
        Warehouse warehouse = domainValidator.getOwnedWarehouse(userId, warehouseId);
        warehouse.deactivate();
    }

    /**
     * 창고 재활성화
     */
    @Transactional
    public void activateWarehouse(Long userId, Long warehouseId) {
        Warehouse warehouse = domainValidator.getOwnedWarehouse(userId, warehouseId);
        warehouse.activate();
    }
}
