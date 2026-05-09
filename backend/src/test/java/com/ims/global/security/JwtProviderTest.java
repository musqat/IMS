package com.ims.global.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JwtProviderTest {

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-key-must-be-at-least-32-bytes!!");
        props.setAccessTokenExpiry(3600_000L);   // 1시간
        props.setRefreshTokenExpiry(1_209_600_000L); // 2주
        jwtProvider = new JwtProvider(props);
    }

    @Test
    @DisplayName("Access 토큰 생성 및 userId 추출 성공")
    void generateAccessToken_and_getUserId() {
        String token = jwtProvider.generateAccessToken(42L);

        assertThat(token).isNotBlank();
        assertThat(jwtProvider.getUserId(token)).isEqualTo(42L);
    }

    @Test
    @DisplayName("Refresh 토큰 생성 및 isRefreshToken 확인")
    void generateRefreshToken_isRefreshToken() {
        String token = jwtProvider.generateRefreshToken(42L);

        assertThat(jwtProvider.isRefreshToken(token)).isTrue();
    }

    @Test
    @DisplayName("Access 토큰은 isRefreshToken = false")
    void accessToken_isNotRefreshToken() {
        String token = jwtProvider.generateAccessToken(1L);

        assertThat(jwtProvider.isRefreshToken(token)).isFalse();
    }

    @Test
    @DisplayName("유효한 토큰 → isValid = true")
    void isValid_validToken() {
        String token = jwtProvider.generateAccessToken(1L);

        assertThat(jwtProvider.isValid(token)).isTrue();
    }

    @Test
    @DisplayName("변조된 토큰 → isValid = false")
    void isValid_tamperedToken() {
        assertThat(jwtProvider.isValid("not.a.valid.token")).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰 → isValid = false")
    void isValid_expiredToken() {
        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-key-must-be-at-least-32-bytes!!");
        props.setAccessTokenExpiry(-1000L); // 이미 만료
        props.setRefreshTokenExpiry(-1000L);
        JwtProvider expiredProvider = new JwtProvider(props);

        String token = expiredProvider.generateAccessToken(1L);

        assertThat(jwtProvider.isValid(token)).isFalse();
    }
}
