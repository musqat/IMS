package com.ims.partnership.service;

import com.ims.global.exception.ImsException;
import com.ims.partnership.dto.request.InviteRequest;
import com.ims.partnership.dto.response.PartnershipResponse;
import com.ims.partnership.entity.Partnership;
import com.ims.partnership.entity.Partnership.PartnershipStatus;
import com.ims.partnership.repository.PartnershipRepository;
import com.ims.user.entity.User;
import com.ims.user.repository.UserRepository;
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
class PartnershipServiceTest {

    @InjectMocks
    private PartnershipService partnershipService;

    @Mock
    private PartnershipRepository partnershipRepository;

    @Mock
    private UserRepository userRepository;

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
        given(userRepository.findById(1L)).willReturn(Optional.of(mainUser));
        given(userRepository.findByCompanyCode("2000000001")).willReturn(Optional.of(subUser));
        given(partnershipRepository.existsByMainIdAndSubId(1L, 2L)).willReturn(false);
        given(partnershipRepository.existsByMainIdAndSubId(2L, 1L)).willReturn(false);
        given(partnershipRepository.save(any())).willAnswer(i -> i.getArgument(0));

        String token = partnershipService.invite(1L, request);

        assertThat(token).isNotBlank();
        then(partnershipRepository).should().save(any(Partnership.class));
    }

    @Test
    @DisplayName("초대 실패 - 자기 자신 초대")
    void invite_selfInvite() {
        InviteRequest request = new InviteRequest("1000000001");
        given(userRepository.findById(1L)).willReturn(Optional.of(mainUser));
        given(userRepository.findByCompanyCode("1000000001")).willReturn(Optional.of(mainUser));

        assertThatThrownBy(() -> partnershipService.invite(1L, request))
                .isInstanceOf(ImsException.class);
    }

    @Test
    @DisplayName("초대 실패 - 이미 존재하는 파트너십")
    void invite_duplicatePartnership() {
        InviteRequest request = new InviteRequest("2000000001");
        given(userRepository.findById(1L)).willReturn(Optional.of(mainUser));
        given(userRepository.findByCompanyCode("2000000001")).willReturn(Optional.of(subUser));
        given(partnershipRepository.existsByMainIdAndSubId(1L, 2L)).willReturn(true);

        assertThatThrownBy(() -> partnershipService.invite(1L, request))
                .isInstanceOf(ImsException.class);
    }

    @Test
    @DisplayName("초대 실패 - 역방향 파트너십 이미 존재")
    void invite_reversePartnershipExists() {
        InviteRequest request = new InviteRequest("2000000001");
        given(userRepository.findById(1L)).willReturn(Optional.of(mainUser));
        given(userRepository.findByCompanyCode("2000000001")).willReturn(Optional.of(subUser));
        given(partnershipRepository.existsByMainIdAndSubId(1L, 2L)).willReturn(false);
        given(partnershipRepository.existsByMainIdAndSubId(2L, 1L)).willReturn(true);

        assertThatThrownBy(() -> partnershipService.invite(1L, request))
                .isInstanceOf(ImsException.class);
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
                .isInstanceOf(ImsException.class);
    }

    @Test
    @DisplayName("수락 실패 - 유효하지 않은 토큰")
    void accept_invalidToken() {
        given(partnershipRepository.findByInviteToken("bad-token")).willReturn(Optional.empty());

        assertThatThrownBy(() -> partnershipService.accept(2L, "bad-token"))
                .isInstanceOf(ImsException.class);
    }

    @Test
    @DisplayName("수락 실패 - 이미 수락된 초대")
    void accept_alreadyAccepted() {
        Partnership accepted = Partnership.builder()
                .id(1L).main(mainUser).sub(subUser)
                .status(PartnershipStatus.ACCEPTED).inviteToken("valid-token").build();
        given(partnershipRepository.findByInviteToken("valid-token")).willReturn(Optional.of(accepted));

        assertThatThrownBy(() -> partnershipService.accept(2L, "valid-token"))
                .isInstanceOf(ImsException.class);
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
}
