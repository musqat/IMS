package com.ims.partnership.repository;

import com.ims.partnership.entity.Partnership;
import com.ims.partnership.entity.Partnership.PartnershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PartnershipRepository extends JpaRepository<Partnership, Long> {

    // 초대 토큰으로 조회 (수락 시 사용)
    Optional<Partnership> findByInviteToken(String inviteToken);

    // 본사 기준 하청 목록 (ACCEPTED만)
    List<Partnership> findAllByMainIdAndStatus(Long mainId, PartnershipStatus status);

    // 하청 기준 본사 목록 (ACCEPTED만)
    List<Partnership> findAllBySubIdAndStatus(Long subId, PartnershipStatus status);

    // 중복 초대 방지 (PENDING 포함 이미 관계 존재 여부)
    boolean existsByMainIdAndSubId(Long mainId, Long subId);

    // 두 User 간 ACCEPTED 관계 존재 여부 (창고 공유 시 검증용)
    boolean existsByMainIdAndSubIdAndStatus(Long mainId, Long subId, PartnershipStatus status);
}
