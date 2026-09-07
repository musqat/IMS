package com.ims.user.service;

import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    @InjectMocks
    private LoginAttemptService loginAttemptService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private static final String EMAIL = "a@ims.dev";
    private static final String KEY = "login:fail:" + EMAIL;


    @Test
    @DisplayName("실패 기록이 없으면 통과한다")
    void checkNotLocked_noRecord() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(KEY)).willReturn(null);

        // when & then
        assertThatCode(() -> loginAttemptService.checkNotLocked(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("한도 직전까지는 통과한다")
    void checkNotLocked_belowLimit() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(KEY)).willReturn(String.valueOf(LoginAttemptService.MAX_ATTEMPTS - 1));

        // when & then
        assertThatCode(() -> loginAttemptService.checkNotLocked(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("한도에 닿으면 잠긴다")
    void checkNotLocked_atLimit() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(KEY)).willReturn(String.valueOf(LoginAttemptService.MAX_ATTEMPTS));

        // when & then
        assertThatThrownBy(() -> loginAttemptService.checkNotLocked(EMAIL))
                .isInstanceOf(ImsException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOGIN_ATTEMPTS_EXCEEDED);
    }

    /** TTL을 매번 다시 걸어야 잠긴 뒤 계속 시도할 때 잠금이 연장된다 */
    @Test
    @DisplayName("실패를 세면서 TTL을 다시 건다")
    void recordFailure_setsTtl() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        loginAttemptService.recordFailure(EMAIL);

        // then
        then(valueOperations).should().increment(KEY);
        then(redisTemplate).should().expire(KEY, Duration.ofMinutes(10));
    }

    @Test
    @DisplayName("성공하면 카운터를 지운다")
    void reset_deletesKey() {
        // when
        loginAttemptService.reset(EMAIL);

        // then
        then(redisTemplate).should().delete(KEY);
    }
}
