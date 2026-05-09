package com.ims.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private static final int MIN_SECRET_BYTES = 32;

    private final JwtProperties jwtProperties;

    @PostConstruct
    public void validateSecret() {
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes (got " +
                    (secret == null ? 0 : secret.getBytes(StandardCharsets.UTF_8).length) + ")");
        }
    }

    public String generateAccessToken(Long userId) {
        return Jwts.builder()
                .claim("userId", userId)
                .claim("tokenType", "ACCESS")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessTokenExpiry()))
                .signWith(getKey())
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        return Jwts.builder()
                .claim("userId", userId)
                .claim("tokenType", "REFRESH")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getRefreshTokenExpiry()))
                .signWith(getKey())
                .compact();
    }

    public boolean isRefreshToken(String token) {
        return "REFRESH".equals(parse(token).get("tokenType", String.class));
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        return parse(token).get("userId", Long.class);
    }

    /**
     * 만료된 토큰도 userId 추출 허용 (로그아웃 시 사용)
     * - 서명이 완전히 유효하지 않은 경우 null 반환
     */
    public Long extractUserIdLeniently(String token) {
        try {
            return parse(token).get("userId", Long.class);
        } catch (ExpiredJwtException e) {
            return e.getClaims().get("userId", Long.class);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
