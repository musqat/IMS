package com.ims.user.service;

import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 로그인 시도 제한
 * - 계정별로 실패를 세고 한도를 넘기면 잠근다
 * - 카운터는 Redis에 두고 TTL로 스스로 사라지게 한다. 잠금 해제 배치가 필요 없다
 * - Redis가 죽으면 제한을 포기하고 로그인은 통과시킨다. 카운터를 못 읽는 상태에서
 *   막으면 모든 계정이 동시에 잠겨 장애가 커진다
 */
@Slf4j
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
        try {
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, LOCK_DURATION);
        } catch (DataAccessException e) {
            log.warn("로그인 실패 기록 실패 — 이번 시도는 세지 않는다", e);
        }
    }

    /** 로그인에 성공하면 카운터를 지운다 */
    public void reset(String email) {
        try {
            redisTemplate.delete(KEY_PREFIX + email);
        } catch (DataAccessException e) {
            log.warn("로그인 실패 카운터 삭제 실패 — TTL이 지나면 사라진다", e);
        }
    }

    /** 못 읽으면 0으로 본다. 잠금 검사를 건너뛰는 쪽을 택한다 */
    private long currentAttempts(String email) {
        try {
            String value = redisTemplate.opsForValue().get(KEY_PREFIX + email);
            return value == null ? 0 : Long.parseLong(value);
        } catch (DataAccessException e) {
            log.warn("로그인 실패 카운터 조회 실패 — 시도 제한을 적용하지 않는다", e);
            return 0;
        }
    }
}
