package com.ims.user.service;

import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 로그인 시도 제한
 * - 계정별로 실패를 세고 한도를 넘기면 잠근다
 * - 카운터는 Redis에 두고 TTL로 스스로 사라지게 한다. 잠금 해제 배치가 필요 없다
 */
@Component
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final String KEY_PREFIX = "login:fail:";

    /** 이 횟수까지는 통과. 초과하면 잠근다 */
    static final int MAX_ATTEMPTS = 5;

    /** 마지막 실패로부터 이 시간이 지나면 카운터가 사라진다 */
    static final Duration LOCK_DURATION = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;

    /** 잠긴 계정이면 예외. 로그인 시도 전에 부른다 */
    public void checkNotLocked(String email) {
        if (currentAttempts(email) >= MAX_ATTEMPTS) {
            throw new ImsException(ErrorCode.LOGIN_ATTEMPTS_EXCEEDED);
        }
    }

    /**
     * 실패를 센다.
     * 실패할 때마다 TTL을 다시 건다. 잠긴 뒤에도 계속 시도하면 잠금이 연장된다
     */
    public void recordFailure(String email) {
        String key = KEY_PREFIX + email;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, LOCK_DURATION);
    }

    /** 로그인에 성공하면 카운터를 지운다 */
    public void reset(String email) {
        redisTemplate.delete(KEY_PREFIX + email);
    }

    private long currentAttempts(String email) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + email);
        return value == null ? 0 : Long.parseLong(value);
    }
}
