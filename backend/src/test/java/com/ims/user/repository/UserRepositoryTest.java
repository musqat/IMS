package com.ims.user.repository;

import com.ims.global.config.JpaAuditingConfig;
import com.ims.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("User 저장 후 이메일로 조회 성공")
    void findByEmail_success() {
        // given
        User user = User.builder().email("test@test.com").password("encodedPassword").companyName("테스트회사").build();
        userRepository.save(user);

        // when
        Optional<User> result = userRepository.findByEmail("test@test.com");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("test@test.com");
    }

    @Test
    @DisplayName("존재하는 이메일 - true")
    void existsByEmail_true() {
        // given
        User user = User.builder().email("test@test.com").password("encodedPassword").companyName("테스트회사").build();
        userRepository.save(user);

        // when & then
        assertThat(userRepository.existsByEmail("test@test.com")).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 이메일 - false")
    void existsByEmail_false() {
        // when & then
        assertThat(userRepository.existsByEmail("no@test.com")).isFalse();
    }
}
