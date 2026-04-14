package com.ims.global.security;

import com.ims.global.common.Role;
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

    public String generateAccessToken(Long userId, Role role) {
        return generate(userId, role.name(), jwtProperties.getAccessTokenExpiry());
    }

    public String generateRefreshToken(Long userId) {
        return generate(userId, null, jwtProperties.getRefreshTokenExpiry());
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

    private String generate(Long userId, String role, long expiryMs) {
        var builder = Jwts.builder()
                .claim("userId", userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiryMs))
                .signWith(getKey());

        if (role != null) {
            builder.claim("role", role);
        }

        return builder.compact();
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
