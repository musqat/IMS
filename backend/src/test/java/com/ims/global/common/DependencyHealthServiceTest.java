package com.ims.global.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class DependencyHealthServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private RedisConnectionFactory connectionFactory;

    @Mock
    private RedisConnection connection;

    @InjectMocks
    private DependencyHealthService dependencyHealthService;

    private void givenDbUp() {
        given(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).willReturn(1);
    }

    private void givenDbDown() {
        given(jdbcTemplate.queryForObject("SELECT 1", Integer.class))
                .willThrow(new DataAccessResourceFailureException("연결 실패"));
    }

    private void givenRedisUp() {
        given(redisTemplate.getConnectionFactory()).willReturn(connectionFactory);
        given(connectionFactory.getConnection()).willReturn(connection);
        given(connection.ping()).willReturn("PONG");
    }

    @Test
    @DisplayName("둘 다 살아 있으면 UP·UP")
    void bothUp() {
        // given
        givenDbUp();
        givenRedisUp();

        // when
        DepsStatus status = dependencyHealthService.check();

        // then
        assertThat(status.db()).isEqualTo("UP");
        assertThat(status.redis()).isEqualTo("UP");
        assertThat(status.allUp()).isTrue();
    }

    @Test
    @DisplayName("쿼리가 실패하면 db만 DOWN")
    void dbDown() {
        // given
        givenDbDown();
        givenRedisUp();

        // when
        DepsStatus status = dependencyHealthService.check();

        // then
        assertThat(status.db()).isEqualTo("DOWN");
        assertThat(status.redis()).isEqualTo("UP");
        assertThat(status.allUp()).isFalse();
    }

    @Test
    @DisplayName("PING이 실패하면 redis만 DOWN")
    void redisDown() {
        // given
        givenDbUp();
        given(redisTemplate.getConnectionFactory()).willReturn(connectionFactory);
        given(connectionFactory.getConnection())
                .willThrow(new RedisConnectionFailureException("연결 실패"));

        // when
        DepsStatus status = dependencyHealthService.check();

        // then
        assertThat(status.db()).isEqualTo("UP");
        assertThat(status.redis()).isEqualTo("DOWN");
    }

    @Test
    @DisplayName("커넥션 팩토리가 없으면 redis는 DOWN")
    void redisFactoryMissing() {
        // given
        givenDbUp();
        given(redisTemplate.getConnectionFactory()).willReturn(null);

        // when
        DepsStatus status = dependencyHealthService.check();

        // then
        assertThat(status.redis()).isEqualTo("DOWN");
    }

    @Test
    @DisplayName("DB가 죽어도 Redis는 확인한다")
    void dbDownStillChecksRedis() {
        // given
        givenDbDown();
        givenRedisUp();

        // when
        dependencyHealthService.check();

        // then
        then(connection).should().ping();
    }

    @Test
    @DisplayName("확인에 쓴 커넥션은 닫는다")
    void closesConnection() {
        // given
        givenDbUp();
        givenRedisUp();

        // when
        dependencyHealthService.check();

        // then
        then(connection).should().close();
    }
}
