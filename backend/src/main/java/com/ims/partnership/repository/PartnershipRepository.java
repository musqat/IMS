package com.ims.partnership.repository;

import com.ims.partnership.entity.Partnership;
import com.ims.partnership.entity.Partnership.PartnershipStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PartnershipRepository extends JpaRepository<Partnership, Long> {

    /** 초대 토큰으로 조회 */
    Optional<Partnership> findByInviteToken(String inviteToken);

    /**
     * 내가 초대한 쪽 목록
     * - PartnershipResponse가 main·sub의 회사명을 읽으므로 함께 로딩한다 (없으면 행당 2쿼리)
     */
    @EntityGraph(attributePaths = {"main", "sub"})
    List<Partnership> findAllByMainIdAndStatus(Long mainId, PartnershipStatus status);

    /** 나를 초대한 쪽 목록 */
    @EntityGraph(attributePaths = {"main", "sub"})
    List<Partnership> findAllBySubIdAndStatus(Long subId, PartnershipStatus status);

    /** 중복 초대 방지 (PENDING 포함 이미 관계 존재 여부) */
    boolean existsByMainIdAndSubId(Long mainId, Long subId);

    /** 재초대 판정용 — 만료 여부를 보려면 엔티티가 필요하다 */
    Optional<Partnership> findByMainIdAndSubId(Long mainId, Long subId);

    /** 두 User 간 ACCEPTED 관계 존재 여부 (창고 공유 시 검증용) */
    boolean existsByMainIdAndSubIdAndStatus(Long mainId, Long subId, PartnershipStatus status);
}
