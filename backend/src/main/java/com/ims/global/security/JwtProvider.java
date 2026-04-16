package com.ims.global.security;

import com.ims.global.common.Role;
import com.ims.global.common.UserType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtProperties jwtProperties;

    public String generateAccessToken(Long userId, UserType userType, Role role) {
        var builder = Jwts.builder()
                .claim("userId", userId)
                .claim("userType", userType.name())
                .claim("tokenType", "ACCESS")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessTokenExpiry()))
                .signWith(getKey());

        if (role != null) {
            builder.claim("role", role.name());
        }

        return builder.compact();
    }

    public String generateRefreshToken(Long userId, UserType userType) {
        return Jwts.builder()
                .claim("userId", userId)
                .claim("userType", userType.name())
                .claim("tokenType", "REFRESH")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getRefreshTokenExpiry()))
                .signWith(getKey())
                .compact();
    }

    public boolean isRefreshToken(String token) {
        return "REFRESH".equals(parse(token).get("tokenType", String.class));
    }

    public UserType getUserType(String token) {
        return UserType.valueOf(parse(token).get("userType", String.class));
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
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

    public String getRole(String token) {
        return parse(token).get("role", String.class);
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
