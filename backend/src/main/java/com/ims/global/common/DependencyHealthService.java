package com.ims.global.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * DB·Redis를 한 번씩 건드려 상태를 확인한다.
 *
 * 무료 플랜 Supabase는 활동이 없으면 프로젝트를 일시정지한다.
 * 쿼리 한 번이 활동으로 집계되므로 외부 크론이 하루 한 번 이 경로를 친다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DependencyHealthService {

    private static final String UP = "UP";
    private static final String DOWN = "DOWN";

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;

    /**
     * 두 의존성을 각각 확인한다. 하나가 죽어도 다른 쪽 결과는 남긴다
     */
    public DepsStatus check() {
        return new DepsStatus(
                checkDb() ? UP : DOWN,
                checkRedis() ? UP : DOWN);
    }

    /**
     * 커넥션을 받아 쿼리 하나를 던진다
     */
    private boolean checkDb() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (DataAccessException e) {
            log.warn("DB 상태 확인 실패", e);
            return false;
        }
    }

    /**
     * Redis 커넥션에 PING을 보낸다
     */
    private boolean checkRedis() {
        RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
        if (factory == null) {
            return false;
        }
        try (RedisConnection connection = factory.getConnection()) {
            connection.ping();
            return true;
        } catch (DataAccessException e) {
            log.warn("Redis 상태 확인 실패", e);
            return false;
        }
    }
}
