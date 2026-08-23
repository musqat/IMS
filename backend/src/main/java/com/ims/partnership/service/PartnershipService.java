package com.ims.partnership.service;

import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.partnership.dto.request.InviteRequest;
import com.ims.partnership.dto.response.InviteResponse;
import com.ims.partnership.dto.response.PartnershipResponse;
import com.ims.partnership.entity.Partnership;
import com.ims.partnership.entity.Partnership.PartnershipStatus;
import com.ims.partnership.repository.PartnershipRepository;
import com.ims.user.entity.User;
import com.ims.user.repository.UserRepository;
import com.ims.warehouse.repository.WarehouseShareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartnershipService {

    private final PartnershipRepository partnershipRepository;
    private final UserRepository userRepository;
    private final WarehouseShareRepository warehouseShareRepository;

    /** 초대 링크 유효 기간. 오래된 링크가 언제든 수락되는 것을 막는다 */
    private static final int INVITE_VALID_DAYS = 7;

    private LocalDateTime newExpiry() {
        return LocalDateTime.now().plusDays(INVITE_VALID_DAYS);
    }

    /**
     * 파트너 초대 발송
     * - companyCode로 상대 User 조회
     * - 자기 자신 초대 및 이미 관계 존재 여부 검증
     * - UUID 토큰 생성 후 Partnership(PENDING) 저장
     */
    @Transactional
    public InviteResponse invite(Long mainId, InviteRequest request) {
        // mainId는 JWT에서 추출된 인증된 userId — getReferenceById로 프록시만 사용
        User user = userRepository.getReferenceById(mainId);
        User inviteUser = userRepository.findByCompanyCode(request.companyCode()).orElseThrow(() -> new ImsException(ErrorCode.USER_NOT_FOUND));

        if (user.getId().equals(inviteUser.getId())) {
            throw new ImsException(ErrorCode.SELF_INVITE);
        }

        // 역방향 관계가 이미 있으면 방향과 무관하게 중복이다
        if (partnershipRepository.existsByMainIdAndSubId(inviteUser.getId(), user.getId())) {
            throw new ImsException(ErrorCode.DUPLICATE_PARTNERSHIP);
        }

        // 만료된 초대는 토큰을 새로 발급해 되살린다.
        // UK가 (main_id, sub_id)라 새 행을 만들 수 없어, 중복으로 막으면 그 관계는 영영 초대할 수 없다
        var existing = partnershipRepository.findByMainIdAndSubId(user.getId(), inviteUser.getId());
        if (existing.isPresent()) {
            Partnership found = existing.get();
            if (found.getStatus() == PartnershipStatus.ACCEPTED || !found.isInviteExpired()) {
                throw new ImsException(ErrorCode.DUPLICATE_PARTNERSHIP);
            }
            found.reissueInvite(UUID.randomUUID().toString(), newExpiry());
            return new InviteResponse(found.getId(), found.getInviteToken());
        }

        String token = UUID.randomUUID().toString();

        Partnership partnership = Partnership.builder()
                .main(user)
                .sub(inviteUser)
                .status(PartnershipStatus.PENDING)
                .inviteToken(token)
                .inviteExpiresAt(newExpiry())
                .build();

        Partnership saved = partnershipRepository.save(partnership);
        return new InviteResponse(saved.getId(), saved.getInviteToken());
    }

    /**
     * PENDING 초대 취소
     * - 초대를 보낸 쪽만 취소 가능
     * - PENDING 상태인 경우만 취소 가능 (ACCEPTED는 removePartnership 사용)
     */
    @Transactional
    public void cancelInvite(Long mainId, Long partnershipId) {
        Partnership partnership = partnershipRepository.findById(partnershipId)
                .orElseThrow(() -> new ImsException(ErrorCode.PARTNERSHIP_NOT_FOUND));
        if (!partnership.getMain().getId().equals(mainId)) {
            throw new ImsException(ErrorCode.FORBIDDEN);
        }
        if (partnership.getStatus() == PartnershipStatus.ACCEPTED) {
            throw new ImsException(ErrorCode.ALREADY_ACCEPTED);
        }
        partnershipRepository.delete(partnership);
    }

    /**
     * 초대 토큰으로 수락
     * - 수신함이 생긴 뒤에도 남긴다. 토큰만 받은 경우의 폴백이다
     */
    @Transactional
    public PartnershipResponse accept(Long subId, String token) {
        Partnership partnership = partnershipRepository.findByInviteToken(token)
                .orElseThrow(() -> new ImsException(ErrorCode.INVALID_INVITE_TOKEN));

        validateAcceptable(partnership, subId);
        partnership.accept();

        return PartnershipResponse.from(partnership);
    }

    /**
     * 수신함에서 초대를 수락
     * - 토큰 없이 partnershipId로 수락한다.
     *   sub_id는 초대 시점에 이미 고정되므로 JWT의 subId와 대조하면 토큰과 같은 역할을 한다
     */
    @Transactional
    public PartnershipResponse acceptById(Long subId, Long partnershipId) {
        Partnership partnership = partnershipRepository.findById(partnershipId)
                .orElseThrow(() -> new ImsException(ErrorCode.PARTNERSHIP_NOT_FOUND));

        validateAcceptable(partnership, subId);
        partnership.accept();

        return PartnershipResponse.from(partnership);
    }

    /**
     * 수락 가능 여부 검증
     * - accept(token)과 acceptById(id)가 공유한다.
     *   진입 경로만 다르고 검증 규칙은 같아야 하므로 한 곳에 둔다
     * - 본인 대상인지 / 이미 수락됐는지 / 만료됐는지
     * - 소유자 확인이 먼저다. 순서가 바뀌면 남의 초대라도 상태 응답으로 존재 여부를 알 수 있다
     */
    private void validateAcceptable(Partnership partnership, Long subId) {
        if (!partnership.getSub().getId().equals(subId)) {
            throw new ImsException(ErrorCode.FORBIDDEN);
        }
        if (partnership.getStatus() == PartnershipStatus.ACCEPTED) {
            throw new ImsException(ErrorCode.ALREADY_ACCEPTED);
        }
        if (partnership.isInviteExpired()) {
            throw new ImsException(ErrorCode.EXPIRED_INVITE_TOKEN);
        }
    }

    /**
     * 내가 받은 PENDING 초대 목록
     * - 만료된 초대도 포함한다. 목록에서 사라지면 초대가 있었다는 사실조차 알 수 없다.
     *   만료 표시는 inviteExpiresAt으로 화면에서 판단한다
     */
    public List<PartnershipResponse> getReceivedInvites(Long subId) {
        return partnershipRepository.findAllBySubIdAndStatus(subId, PartnershipStatus.PENDING).stream().map(PartnershipResponse::from).toList();
    }

    /**
     * 내가 보낸 PENDING 초대 목록
     * - 보낸 초대가 안 보이면 수락 여부를 알 수 없어 계속 재초대하게 된다
     */
    public List<PartnershipResponse> getSentInvites(Long mainId) {
        return partnershipRepository.findAllByMainIdAndStatus(mainId, PartnershipStatus.PENDING).stream().map(PartnershipResponse::from).toList();
    }

    /**
     * 내가 초대해서 맺어진 ACCEPTED 파트너 목록
     */
    public List<PartnershipResponse> getSubList(Long mainId) {
        return partnershipRepository.findAllByMainIdAndStatus(mainId, PartnershipStatus.ACCEPTED).stream().map(PartnershipResponse::from).toList();
    }

    /**
     * 나를 초대해서 맺어진 ACCEPTED 파트너 목록
     */
    public List<PartnershipResponse> getMainList(Long subId) {
        return partnershipRepository.findAllBySubIdAndStatus(subId, PartnershipStatus.ACCEPTED).stream().map(PartnershipResponse::from).toList();
    }

    /**
     * 파트너십에 별명(alias) 설정
     */
    @Transactional
    public PartnershipResponse updateAlias(Long userId, Long partnershipId, String alias) {
        Partnership partnership = partnershipRepository.findById(partnershipId)
                .orElseThrow(() -> new ImsException(ErrorCode.PARTNERSHIP_NOT_FOUND));

        boolean isMember = partnership.getMain().getId().equals(userId)
                || partnership.getSub().getId().equals(userId);
        if (!isMember) {
            throw new ImsException(ErrorCode.FORBIDDEN);
        }
        if (partnership.getStatus() != PartnershipStatus.ACCEPTED) {
            throw new ImsException(ErrorCode.PARTNERSHIP_NOT_ACCEPTED);
        }

        partnership.updateAlias(alias);
        return PartnershipResponse.from(partnership);
    }

    /**
     * 파트너십 해제
     */
    @Transactional
    public void removePartnership(Long userId, Long partnershipId) {
        Partnership partnership = partnershipRepository.findById(partnershipId).orElseThrow(() -> new ImsException(ErrorCode.PARTNERSHIP_NOT_FOUND));
        boolean isMember = partnership.getMain().getId().equals(userId) || partnership.getSub().getId().equals(userId);
        if (!isMember) {
            throw new ImsException(ErrorCode.FORBIDDEN);
        }
        // PENDING은 cancelInvite로만 취소 가능 — removePartnership은 ACCEPTED만 처리
        if (partnership.getStatus() != PartnershipStatus.ACCEPTED) {
            throw new ImsException(ErrorCode.PARTNERSHIP_NOT_ACCEPTED);
        }

        Long mainUserId = partnership.getMain().getId();
        Long subUserId  = partnership.getSub().getId();
        partnershipRepository.delete(partnership);
        // 파트너십 해제 시 양방향 창고 공유도 함께 정리
        warehouseShareRepository.deleteByWarehouseOwnerIdAndSharedWithId(mainUserId, subUserId);
        warehouseShareRepository.deleteByWarehouseOwnerIdAndSharedWithId(subUserId, mainUserId);
    }

    /**
     * 두 User 간 ACCEPTED Partnership 존재 여부 확인
     * - main-sub, sub-main 양방향 모두 확인
     */
    public boolean isPartner(Long userId, Long targetId) {
        return partnershipRepository.existsByMainIdAndSubIdAndStatus(userId, targetId, PartnershipStatus.ACCEPTED)
                || partnershipRepository.existsByMainIdAndSubIdAndStatus(targetId, userId, PartnershipStatus.ACCEPTED);
    }
}
