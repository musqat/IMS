package com.ims.partnership.repository;

import com.ims.global.config.JpaAuditingConfig;
import com.ims.partnership.entity.Partnership;
import com.ims.partnership.entity.Partnership.PartnershipStatus;
import com.ims.user.entity.User;
import com.ims.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(JpaAuditingConfig.class)
class PartnershipRepositoryTest {

    @Autowired
    private PartnershipRepository partnershipRepository;

    @Autowired
    private UserRepository userRepository;

    private User main;
    private User sub;

    @BeforeEach
    void setUp() {
        main = userRepository.save(User.builder()
                .email("main@test.com").password("pw").companyName("본사").companyCode("1000000001").build());
        sub = userRepository.save(User.builder()
                .email("sub@test.com").password("pw").companyName("하청").companyCode("2000000002").build());
    }

    @Test
    @DisplayName("초대 토큰으로 조회 성공")
    void findByInviteToken_success() {
        partnershipRepository.save(Partnership.builder()
                .main(main).sub(sub).status(PartnershipStatus.PENDING).inviteToken("token-uuid").build());

        Optional<Partnership> result = partnershipRepository.findByInviteToken("token-uuid");

        assertThat(result).isPresent();
        assertThat(result.get().getMain().getId()).isEqualTo(main.getId());
    }

    @Test
    @DisplayName("초대 토큰으로 조회 실패 - 없는 토큰")
    void findByInviteToken_notFound() {
        Optional<Partnership> result = partnershipRepository.findByInviteToken("invalid-token");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("중복 관계 존재 여부 - true")
    void existsByMainIdAndSubId_true() {
        partnershipRepository.save(Partnership.builder()
                .main(main).sub(sub).status(PartnershipStatus.PENDING).inviteToken("token-uuid").build());

        assertThat(partnershipRepository.existsByMainIdAndSubId(main.getId(), sub.getId())).isTrue();
    }

    @Test
    @DisplayName("중복 관계 존재 여부 - false")
    void existsByMainIdAndSubId_false() {
        assertThat(partnershipRepository.existsByMainIdAndSubId(main.getId(), sub.getId())).isFalse();
    }

    @Test
    @DisplayName("본사 기준 ACCEPTED 하청 목록 조회")
    void findAllByMainIdAndStatus_accepted() {
        partnershipRepository.save(Partnership.builder()
                .main(main).sub(sub).status(PartnershipStatus.ACCEPTED).inviteToken("token-uuid").build());

        List<Partnership> result = partnershipRepository.findAllByMainIdAndStatus(main.getId(), PartnershipStatus.ACCEPTED);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSub().getId()).isEqualTo(sub.getId());
    }

    @Test
    @DisplayName("하청 기준 ACCEPTED 본사 목록 조회")
    void findAllBySubIdAndStatus_accepted() {
        partnershipRepository.save(Partnership.builder()
                .main(main).sub(sub).status(PartnershipStatus.ACCEPTED).inviteToken("token-uuid").build());

        List<Partnership> result = partnershipRepository.findAllBySubIdAndStatus(sub.getId(), PartnershipStatus.ACCEPTED);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMain().getId()).isEqualTo(main.getId());
    }

    @Test
    @DisplayName("ACCEPTED 관계 존재 여부 - true")
    void existsByMainIdAndSubIdAndStatus_true() {
        partnershipRepository.save(Partnership.builder()
                .main(main).sub(sub).status(PartnershipStatus.ACCEPTED).inviteToken("token-uuid").build());

        assertThat(partnershipRepository.existsByMainIdAndSubIdAndStatus(
                main.getId(), sub.getId(), PartnershipStatus.ACCEPTED)).isTrue();
    }

    @Test
    @DisplayName("ACCEPTED 관계 존재 여부 - PENDING이면 false")
    void existsByMainIdAndSubIdAndStatus_pending() {
        partnershipRepository.save(Partnership.builder()
                .main(main).sub(sub).status(PartnershipStatus.PENDING).inviteToken("token-uuid").build());

        assertThat(partnershipRepository.existsByMainIdAndSubIdAndStatus(
                main.getId(), sub.getId(), PartnershipStatus.ACCEPTED)).isFalse();
    }
}
