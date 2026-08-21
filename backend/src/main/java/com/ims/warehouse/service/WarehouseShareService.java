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
import com.ims.warehouse.repository.WarehouseRepository;
import com.ims.warehouse.repository.WarehouseShareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WarehouseShareService {

    private final WarehouseShareRepository warehouseShareRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;
    private final PartnershipService partnershipService;
    private final DomainValidator domainValidator;

    /**
     * 창고 공유 부여
     * - 창고 소유자 검증
     * - Partnership 관계 검증 (ACCEPTED 상태여야 함)
     * - 이미 공유된 경우 권한 업데이트, 아니면 신규 생성
     */
    @Transactional
    public WarehouseShareResponse share(Long userId, Long warehouseId, ShareRequest request) {
        Warehouse warehouse = domainValidator.getOwnedWarehouse(userId, warehouseId);
        User target = userRepository.findByCompanyCode(request.companyCode())
                .orElseThrow(() -> new ImsException(ErrorCode.USER_NOT_FOUND));

        if (!partnershipService.isPartner(userId, target.getId())) {
            throw new ImsException(ErrorCode.NOT_PARTNER);
        }

        Optional<WarehouseShare> existing = warehouseShareRepository
                .findByWarehouseIdAndSharedWithId(warehouseId, target.getId());

        if (existing.isPresent()) {
            WarehouseShare share = existing.get();
            share.updatePermission(request.permission());
            return WarehouseShareResponse.from(warehouseShareRepository.save(share));
        }

        WarehouseShare warehouseShare = WarehouseShare.builder()
                .warehouse(warehouse)
                .sharedWith(target)
                .permission(request.permission())
                .build();

        return WarehouseShareResponse.from(warehouseShareRepository.save(warehouseShare));
    }

    /**
     * 공유 권한 회수
     * - 창고 소유자 검증
     * - WarehouseShare 삭제
     */
    @Transactional
    public void revoke(Long userId, Long warehouseId, String companyCode) {
        domainValidator.getOwnedWarehouse(userId, warehouseId);

        User target = userRepository.findByCompanyCode(companyCode)
                .orElseThrow(() -> new ImsException(ErrorCode.USER_NOT_FOUND));

        WarehouseShare share = warehouseShareRepository
                .findByWarehouseIdAndSharedWithId(warehouseId, target.getId())
                .orElseThrow(() -> new ImsException(ErrorCode.WAREHOUSE_SHARE_NOT_FOUND));

        warehouseShareRepository.delete(share);
    }

    /**
     * 공유받은 창고 목록 조회
     * - sharedWithId 기준 전체 반환
     */
    public List<WarehouseShareResponse> getSharedWarehouses(Long userId) {
        return warehouseShareRepository.findAllBySharedWithId(userId).stream()
                .map(WarehouseShareResponse::from)
                .toList();
    }

    /**
     * userId가 접근 가능한 창고 ID 목록 반환 (소유 + 공유받은 창고)
     * - ProductionService 등에서 복잡한 EXISTS 서브쿼리 대신 IN 절로 사용
     */
    public List<Long> getAccessibleWarehouseIds(Long userId) {
        List<Long> owned = warehouseRepository.findAllByOwnerId(userId)
                .stream().map(Warehouse::getId).toList();
        List<Long> shared = warehouseShareRepository.findAllBySharedWithId(userId)
                .stream().map(ws -> ws.getWarehouse().getId()).toList();
        return Stream.concat(owned.stream(), shared.stream()).distinct().toList();
    }

    /**
     * 창고 FULL 권한 검증 (다른 서비스에서 호출)
     * - 소유자면 통과
     * - WarehouseShare에 FULL 권한 있으면 통과
     * - 없으면 접근 거부 예외
     */
    public void checkFullAccess(Long userId, Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ImsException(ErrorCode.WAREHOUSE_NOT_FOUND));

        if (warehouse.getOwner().getId().equals(userId)) return;

        WarehouseShare share = warehouseShareRepository
                .findByWarehouseIdAndSharedWithId(warehouseId, userId)
                .orElseThrow(() -> new ImsException(ErrorCode.WAREHOUSE_ACCESS_DENIED));

        if (share.getPermission() != WarehouseShare.SharePermission.FULL) {
            throw new ImsException(ErrorCode.WAREHOUSE_ACCESS_DENIED);
        }
    }

    /**
     * 창고 조회 권한 검증 (다른 서비스에서 호출)
     * - 소유자면 통과
     * - WarehouseShare에 VIEW 또는 FULL 권한 있으면 통과
     * - 검증한 창고를 반환한다. 품목·BOM처럼 창고 소유자 기준으로 조회해야 하는
     *   후속 작업에서 소유자를 다시 조회하지 않도록 하기 위함이다
     */
    public Warehouse checkViewAccess(Long userId, Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ImsException(ErrorCode.WAREHOUSE_NOT_FOUND));

        if (warehouse.getOwner().getId().equals(userId)) return warehouse;

        warehouseShareRepository.findByWarehouseIdAndSharedWithId(warehouseId, userId)
                .orElseThrow(() -> new ImsException(ErrorCode.WAREHOUSE_ACCESS_DENIED));
        return warehouse;
    }

}
