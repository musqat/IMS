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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PartnershipServiceTest {

    @InjectMocks
    private PartnershipService partnershipService;

    @Mock
    private PartnershipRepository partnershipRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WarehouseShareRepository warehouseShareRepository;

    private User mainUser;
    private User subUser;

    @BeforeEach
    void setUp() {
        mainUser = User.builder()
                .id(1L)
                .email("main@test.com")
                .password("encoded")
                .companyName("본사")
                .companyCode("1000000001")
                .build();

        subUser = User.builder()
                .id(2L)
                .email("sub@test.com")
                .password("encoded")
                .companyName("하청")
                .companyCode("2000000001")
                .build();
    }

    @Test
    @DisplayName("초대 성공 - 토큰 반환")
    void invite_success() {
        InviteRequest request = new InviteRequest("2000000001");
        // mainId는 JWT 인증 userId → getReferenceById 프록시 사용
        given(userRepository.getReferenceById(1L)).willReturn(mainUser);
        given(userRepository.findByCompanyCode("2000000001")).willReturn(Optional.of(subUser));
        given(partnershipRepository.existsByMainIdAndSubId(2L, 1L)).willReturn(false);
        given(partnershipRepository.findByMainIdAndSubId(1L, 2L)).willReturn(Optional.empty());
        given(partnershipRepository.save(any())).willAnswer(i -> i.getArgument(0));

        InviteResponse response = partnershipService.invite(1L, request);

        assertThat(response.inviteToken()).isNotBlank();
        then(partnershipRepository).should().save(any(Partnership.class));
    }

    @Test
    @DisplayName("초대 실패 - 자기 자신 초대")
    void invite_selfInvite() {
        InviteRequest request = new InviteRequest("1000000001");
        given(userRepository.getReferenceById(1L)).willReturn(mainUser);
        given(userRepository.findByCompanyCode("1000000001")).willReturn(Optional.of(mainUser));

        assertThatThrownBy(() -> partnershipService.invite(1L, request))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SELF_INVITE);
    }

    @Test
    @DisplayName("초대 실패 - 이미 존재하는 파트너십")
    void invite_duplicatePartnership() {
        InviteRequest request = new InviteRequest("2000000001");
        given(userRepository.getReferenceById(1L)).willReturn(mainUser);
        given(userRepository.findByCompanyCode("2000000001")).willReturn(Optional.of(subUser));
        given(partnershipRepository.existsByMainIdAndSubId(2L, 1L)).willReturn(false);
        given(partnershipRepository.findByMainIdAndSubId(1L, 2L)).willReturn(Optional.of(
                Partnership.builder().id(1L).main(mainUser).sub(subUser)
                        .status(PartnershipStatus.ACCEPTED).build()));

        assertThatThrownBy(() -> partnershipService.invite(1L, request))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_PARTNERSHIP);
    }

    @Test
    @DisplayName("초대 실패 - 역방향 파트너십 이미 존재")
    void invite_reversePartnershipExists() {
        InviteRequest request = new InviteRequest("2000000001");
        given(userRepository.getReferenceById(1L)).willReturn(mainUser);
        given(userRepository.findByCompanyCode("2000000001")).willReturn(Optional.of(subUser));
        given(partnershipRepository.existsByMainIdAndSubId(2L, 1L)).willReturn(true);

        assertThatThrownBy(() -> partnershipService.invite(1L, request))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_PARTNERSHIP);
    }

    @Test
    @DisplayName("수락 성공")
    void accept_success() {
        Partnership pending = Partnership.builder()
                .id(1L).main(mainUser).sub(subUser)
                .status(PartnershipStatus.PENDING).inviteToken("valid-token").build();
        given(partnershipRepository.findByInviteToken("valid-token")).willReturn(Optional.of(pending));

        PartnershipResponse response = partnershipService.accept(2L, "valid-token");

        assertThat(response.status()).isEqualTo("ACCEPTED");
    }

    @Test
    @DisplayName("수락 실패 - 본인이 아닌 User가 수락 시도")
    void accept_wrongUser() {
        Partnership pending = Partnership.builder()
                .id(1L).main(mainUser).sub(subUser)
                .status(PartnershipStatus.PENDING).inviteToken("valid-token").build();
        given(partnershipRepository.findByInviteToken("valid-token")).willReturn(Optional.of(pending));

        assertThatThrownBy(() -> partnershipService.accept(3L, "valid-token"))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("수락 실패 - 유효하지 않은 토큰")
    void accept_invalidToken() {
        given(partnershipRepository.findByInviteToken("bad-token")).willReturn(Optional.empty());

        assertThatThrownBy(() -> partnershipService.accept(2L, "bad-token"))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INVITE_TOKEN);
    }

    @Test
    @DisplayName("수락 실패 - 이미 수락된 초대")
    void accept_alreadyAccepted() {
        Partnership accepted = Partnership.builder()
                .id(1L).main(mainUser).sub(subUser)
                .status(PartnershipStatus.ACCEPTED).inviteToken("valid-token").build();
        given(partnershipRepository.findByInviteToken("valid-token")).willReturn(Optional.of(accepted));

        assertThatThrownBy(() -> partnershipService.accept(2L, "valid-token"))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_ACCEPTED);
    }

    @Test
    @DisplayName("하청 목록 조회 성공")
    void getSubList_success() {
        Partnership partnership = Partnership.builder()
                .id(1L).main(mainUser).sub(subUser).status(PartnershipStatus.ACCEPTED).build();
        given(partnershipRepository.findAllByMainIdAndStatus(1L, PartnershipStatus.ACCEPTED))
                .willReturn(List.of(partnership));

        List<PartnershipResponse> result = partnershipService.getSubList(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).subCompanyName()).isEqualTo("하청");
    }

    @Test
    @DisplayName("본사 목록 조회 성공")
    void getMainList_success() {
        Partnership partnership = Partnership.builder()
                .id(1L).main(mainUser).sub(subUser).status(PartnershipStatus.ACCEPTED).build();
        given(partnershipRepository.findAllBySubIdAndStatus(2L, PartnershipStatus.ACCEPTED))
                .willReturn(List.of(partnership));

        List<PartnershipResponse> result = partnershipService.getMainList(2L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).mainCompanyName()).isEqualTo("본사");
    }

    @Test
    @DisplayName("파트너 관계 확인 - main→sub 방향")
    void isPartner_mainToSub() {
        given(partnershipRepository.existsByMainIdAndSubIdAndStatus(1L, 2L, PartnershipStatus.ACCEPTED))
                .willReturn(true);

        assertThat(partnershipService.isPartner(1L, 2L)).isTrue();
    }

    @Test
    @DisplayName("파트너 관계 확인 - sub→main 방향 (역방향도 파트너)")
    void isPartner_subToMain() {
        given(partnershipRepository.existsByMainIdAndSubIdAndStatus(2L, 1L, PartnershipStatus.ACCEPTED))
                .willReturn(false);
        given(partnershipRepository.existsByMainIdAndSubIdAndStatus(1L, 2L, PartnershipStatus.ACCEPTED))
                .willReturn(true);

        assertThat(partnershipService.isPartner(2L, 1L)).isTrue();
    }

    @Test
    @DisplayName("파트너 관계 없음")
    void isPartner_notPartner() {
        given(partnershipRepository.existsByMainIdAndSubIdAndStatus(1L, 2L, PartnershipStatus.ACCEPTED))
                .willReturn(false);
        given(partnershipRepository.existsByMainIdAndSubIdAndStatus(2L, 1L, PartnershipStatus.ACCEPTED))
                .willReturn(false);

        assertThat(partnershipService.isPartner(1L, 2L)).isFalse();
    }

    @Test
    @DisplayName("파트너십 해제 성공 - 본사가 해제, 양방향 WarehouseShare 정리됨")
    void removePartnership_byMain() {
        Partnership partnership = Partnership.builder()
                .id(1L).main(mainUser).sub(subUser).status(PartnershipStatus.ACCEPTED).build();
        given(partnershipRepository.findById(1L)).willReturn(Optional.of(partnership));

        partnershipService.removePartnership(mainUser.getId(), 1L);

        then(partnershipRepository).should().delete(partnership);
        // main→sub 방향, sub→main 방향 양쪽 공유 모두 제거됐는지 검증
        then(warehouseShareRepository).should()
                .deleteByWarehouseOwnerIdAndSharedWithId(mainUser.getId(), subUser.getId());
        then(warehouseShareRepository).should()
                .deleteByWarehouseOwnerIdAndSharedWithId(subUser.getId(), mainUser.getId());
    }

    @Test
    @DisplayName("파트너십 해제 성공 - 하청이 해제, 양방향 WarehouseShare 정리됨")
    void removePartnership_bySub() {
        Partnership partnership = Partnership.builder()
                .id(1L).main(mainUser).sub(subUser).status(PartnershipStatus.ACCEPTED).build();
        given(partnershipRepository.findById(1L)).willReturn(Optional.of(partnership));

        partnershipService.removePartnership(subUser.getId(), 1L);

        then(partnershipRepository).should().delete(partnership);
        then(warehouseShareRepository).should()
                .deleteByWarehouseOwnerIdAndSharedWithId(mainUser.getId(), subUser.getId());
        then(warehouseShareRepository).should()
                .deleteByWarehouseOwnerIdAndSharedWithId(subUser.getId(), mainUser.getId());
    }

    @Test
    @DisplayName("파트너십 해제 실패 - 관계없는 User")
    void removePartnership_notMember() {
        Partnership partnership = Partnership.builder()
                .id(1L).main(mainUser).sub(subUser).status(PartnershipStatus.ACCEPTED).build();
        given(partnershipRepository.findById(1L)).willReturn(Optional.of(partnership));

        assertThatThrownBy(() -> partnershipService.removePartnership(999L, 1L))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
        then(partnershipRepository).should(never()).delete(any());
    }

    @Test
    @DisplayName("파트너십 해제 실패 - 존재하지 않는 파트너십")
    void removePartnership_notFound() {
        given(partnershipRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> partnershipService.removePartnership(mainUser.getId(), 99L))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARTNERSHIP_NOT_FOUND);
    }

    @Test
    @DisplayName("별명 설정 성공")
    void updateAlias_success() {
        Partnership partnership = Partnership.builder()
                .id(1L).main(mainUser).sub(subUser).status(PartnershipStatus.ACCEPTED).build();
        given(partnershipRepository.findById(1L)).willReturn(Optional.of(partnership));

        PartnershipResponse response = partnershipService.updateAlias(mainUser.getId(), 1L, "우리하청");

        assertThat(response.alias()).isEqualTo("우리하청");
    }

    @Test
    @DisplayName("별명 설정 실패 - 관계없는 User")
    void updateAlias_notMember() {
        Partnership partnership = Partnership.builder()
                .id(1L).main(mainUser).sub(subUser).status(PartnershipStatus.ACCEPTED).build();
        given(partnershipRepository.findById(1L)).willReturn(Optional.of(partnership));

        assertThatThrownBy(() -> partnershipService.updateAlias(999L, 1L, "별명"))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("별명 설정 실패 - PENDING 상태 (ACCEPTED 아님)")
    void updateAlias_pendingState() {
        Partnership partnership = Partnership.builder()
                .id(1L).main(mainUser).sub(subUser).status(PartnershipStatus.PENDING).build();
        given(partnershipRepository.findById(1L)).willReturn(Optional.of(partnership));

        assertThatThrownBy(() -> partnershipService.updateAlias(mainUser.getId(), 1L, "별명"))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARTNERSHIP_NOT_ACCEPTED);
    }

    @Test
    @DisplayName("파트너십 해제 실패 - PENDING 상태 (cancelInvite로만 취소 가능)")
    void removePartnership_pendingState() {
        Partnership partnership = Partnership.builder()
                .id(1L).main(mainUser).sub(subUser).status(PartnershipStatus.PENDING).build();
        given(partnershipRepository.findById(1L)).willReturn(Optional.of(partnership));

        assertThatThrownBy(() -> partnershipService.removePartnership(mainUser.getId(), 1L))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARTNERSHIP_NOT_ACCEPTED);
        then(partnershipRepository).should(never()).delete(any());
    }

    @Test
    @DisplayName("초대 취소 성공 - 본사가 PENDING 초대 삭제")
    void cancelInvite_success() {
        Partnership pending = Partnership.builder()
                .id(1L).main(mainUser).sub(subUser).status(PartnershipStatus.PENDING).build();
        given(partnershipRepository.findById(1L)).willReturn(Optional.of(pending));

        partnershipService.cancelInvite(mainUser.getId(), 1L);

        then(partnershipRepository).should().delete(pending);
    }

    @Test
    @DisplayName("초대 취소 실패 - 본사가 아닌 User가 취소 시도")
    void cancelInvite_notMain() {
        Partnership pending = Partnership.builder()
                .id(1L).main(mainUser).sub(subUser).status(PartnershipStatus.PENDING).build();
        given(partnershipRepository.findById(1L)).willReturn(Optional.of(pending));

        assertThatThrownBy(() -> partnershipService.cancelInvite(subUser.getId(), 1L))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
        then(partnershipRepository).should(never()).delete(any());
    }

    @Test
    @DisplayName("초대 취소 실패 - 이미 ACCEPTED 상태")
    void cancelInvite_alreadyAccepted() {
        Partnership accepted = Partnership.builder()
                .id(1L).main(mainUser).sub(subUser).status(PartnershipStatus.ACCEPTED).build();
        given(partnershipRepository.findById(1L)).willReturn(Optional.of(accepted));

        assertThatThrownBy(() -> partnershipService.cancelInvite(mainUser.getId(), 1L))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_ACCEPTED);
        then(partnershipRepository).should(never()).delete(any());
    }

    // ===================== 초대 토큰 만료 =====================
    // 토큰이 영구 유효하면 오래된 링크가 언제든 수락된다.
    // 만료된 초대는 재초대로 되살린다. UK가 (main_id, sub_id)라 새 행을 만들 수 없어
    // 기존 행의 토큰과 만료 시각을 새로 발급한다.

    @Test
    @DisplayName("초대 수락 실패 - 만료된 토큰")
    void accept_expiredToken() {
        Partnership expired = Partnership.builder()
                .id(1L).main(mainUser).sub(subUser)
                .status(PartnershipStatus.PENDING)
                .inviteToken("expired-token")
                .inviteExpiresAt(LocalDateTime.now().minusDays(1))
                .build();
        given(partnershipRepository.findByInviteToken("expired-token")).willReturn(Optional.of(expired));

        assertThatThrownBy(() -> partnershipService.accept(subUser.getId(), "expired-token"))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXPIRED_INVITE_TOKEN);
        assertThat(expired.getStatus()).isEqualTo(PartnershipStatus.PENDING);
    }

    @Test
    @DisplayName("초대 - 만료된 초대가 있으면 토큰을 새로 발급한다")
    void invite_expiredInvite_reissues() {
        // 만료된 PENDING이 남아 있으면 UK 때문에 새 초대를 만들 수 없다.
        // 중복으로 막으면 그 관계는 영영 초대할 수 없게 된다
        Partnership expired = Partnership.builder()
                .id(1L).main(mainUser).sub(subUser)
                .status(PartnershipStatus.PENDING)
                .inviteToken("old-token")
                .inviteExpiresAt(LocalDateTime.now().minusDays(1))
                .build();

        given(userRepository.getReferenceById(mainUser.getId())).willReturn(mainUser);
        given(userRepository.findByCompanyCode("2000000001")).willReturn(Optional.of(subUser));
        given(partnershipRepository.findByMainIdAndSubId(mainUser.getId(), subUser.getId()))
                .willReturn(Optional.of(expired));

        InviteResponse result = partnershipService.invite(mainUser.getId(), new InviteRequest("2000000001"));

        assertThat(result.inviteToken()).isNotEqualTo("old-token");
        assertThat(expired.getInviteExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("초대 실패 - 아직 유효한 초대가 있으면 중복이다")
    void invite_validInviteExists_duplicate() {
        Partnership valid = Partnership.builder()
                .id(1L).main(mainUser).sub(subUser)
                .status(PartnershipStatus.PENDING)
                .inviteToken("valid-token")
                .inviteExpiresAt(LocalDateTime.now().plusDays(3))
                .build();

        given(userRepository.getReferenceById(mainUser.getId())).willReturn(mainUser);
        given(userRepository.findByCompanyCode("2000000001")).willReturn(Optional.of(subUser));
        given(partnershipRepository.findByMainIdAndSubId(mainUser.getId(), subUser.getId()))
                .willReturn(Optional.of(valid));

        assertThatThrownBy(() -> partnershipService.invite(mainUser.getId(), new InviteRequest("2000000001")))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_PARTNERSHIP);
    }
}
