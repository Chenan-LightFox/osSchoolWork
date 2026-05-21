package com.osschoolwork.backend.util;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.osschoolwork.backend.config.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    private final JwtProperties properties;

    @Autowired
    public JwtUtil(JwtProperties properties) {
        this.properties = properties;
    }

    public String generateToken(Long userId) {
        // 生成带过期时间的 JWT，subject 保存 userId
        Instant now = Instant.now();
        Instant exp = now.plus(properties.getExpireMinutes(), ChronoUnit.MINUTES);
        return Jwts.builder()
                .setIssuer(properties.getIssuer())
                .setSubject(String.valueOf(userId))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Long parseUserId(String token) {
        try {
            // 校验签名并解析 token
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            String subject = claims.getSubject();
            return subject == null ? null : Long.valueOf(subject);
        } catch (Exception ex) {
            return null;
        }
    }

    private Key getKey() {
        // 统一从配置的 secret 生成签名密钥
        byte[] bytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(bytes);
    }
}
