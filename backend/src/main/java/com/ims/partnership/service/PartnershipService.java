package com.ims.partnership.service;

import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.partnership.dto.request.InviteRequest;
import com.ims.partnership.dto.response.PartnershipResponse;
import com.ims.partnership.entity.Partnership;
import com.ims.partnership.entity.Partnership.PartnershipStatus;
import com.ims.partnership.repository.PartnershipRepository;
import com.ims.user.entity.User;
import com.ims.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartnershipService {

    private final PartnershipRepository partnershipRepository;
    private final UserRepository userRepository;

    /**
     * 본사가 하청에게 초대 발송
     * - companyCode로 하청 User 조회
     * - 이미 관계 존재 시 예외
     * - 자기 자신 초대 시 예외
     * - UUID 토큰 생성 → Partnership(PENDING) 저장 → 토큰 반환
     */
    @Transactional
    public String invite(Long mainId, InviteRequest request) {
        User user = userRepository.findById(mainId).orElseThrow(() -> new ImsException(ErrorCode.USER_NOT_FOUND));
        User inviteUser = userRepository.findByCompanyCode(request.companyCode()).orElseThrow(() -> new ImsException(ErrorCode.USER_NOT_FOUND));

        if (user.getId().equals(inviteUser.getId())) {
            throw new ImsException(ErrorCode.SELF_INVITE);
        }

        if (partnershipRepository.existsByMainIdAndSubId(user.getId(), inviteUser.getId())
                || partnershipRepository.existsByMainIdAndSubId(inviteUser.getId(), user.getId())) {
            throw new ImsException(ErrorCode.DUPLICATE_PARTNERSHIP);
        }

        String token = UUID.randomUUID().toString();

        Partnership partnership = Partnership.builder()
                .main(user)
                .sub(inviteUser)
                .status(PartnershipStatus.PENDING)
                .inviteToken(token)
                .build();

        partnershipRepository.save(partnership);
        return token;
    }

    /**
     * 하청이 초대 토큰으로 수락
     * - 토큰으로 Partnership 조회 (없으면 INVALID_INVITE_TOKEN 예외)
     * - 이미 ACCEPTED면 예외
     * - partnership.accept() 호출
     */
    @Transactional
    public PartnershipResponse accept(Long subId, String token) {
        Partnership partnership = partnershipRepository.findByInviteToken(token).orElseThrow(() -> new ImsException(ErrorCode.INVALID_INVITE_TOKEN));

        if (!partnership.getSub().getId().equals(subId)) {
            throw new ImsException(ErrorCode.FORBIDDEN);
        }

        if (partnership.getStatus().equals(PartnershipStatus.ACCEPTED)) {
            throw new ImsException(ErrorCode.ALREADY_ACCEPTED);
        }

        partnership.accept();

        return PartnershipResponse.from(partnership);
    }

    /**
     * 본사 기준 ACCEPTED 하청 목록 조회
     */
    public List<PartnershipResponse> getSubList(Long mainId) {
        return partnershipRepository.findAllByMainIdAndStatus(mainId, PartnershipStatus.ACCEPTED).stream().map(PartnershipResponse::from).toList();
    }

    /**
     * 하청 기준 ACCEPTED 본사 목록 조회
     */
    public List<PartnershipResponse> getMainList(Long subId) {
        return partnershipRepository.findAllBySubIdAndStatus(subId, PartnershipStatus.ACCEPTED).stream().map(PartnershipResponse::from).toList();
    }

    /**
     * 두 User 간 ACCEPTED Partnership 존재 여부 확인
     * - main-sub 또는 sub-main 양방향 확인
     */
    public boolean isPartner(Long userId, Long targetId) {
        return partnershipRepository.existsByMainIdAndSubIdAndStatus(userId, targetId, PartnershipStatus.ACCEPTED)
                || partnershipRepository.existsByMainIdAndSubIdAndStatus(targetId, userId, PartnershipStatus.ACCEPTED);
    }
}
