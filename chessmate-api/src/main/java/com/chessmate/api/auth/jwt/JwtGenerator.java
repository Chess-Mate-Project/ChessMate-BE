package com.chessmate.api.auth.jwt;

import com.chessmate.infra_core.entity.Profile;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.security.Key;
import java.util.Date;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class JwtGenerator {

    // 공통 헤더
    private static final Map<String, Object> HEADER = Map.of(
            "typ", "JWT",
            "alg", "HS256"
    );

    // 1) Access Token 생성
    public String generateAccessToken(Key secret, long expMillis, Profile profile) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setHeader(HEADER)
                .setSubject(String.valueOf(profile.getId()))      // sub: 사용자 PK(Id)
                .setExpiration(new Date(now + expMillis))       // 만료시간
                .signWith(secret, SignatureAlgorithm.HS256)     // 서명
                .compact(); // JWT 생성
    }

    // 2) Refresh Token 생성
    public String generateRefreshToken(Key secret, long expMillis, Profile profile) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setHeader(HEADER)
                .setSubject(String.valueOf(profile.getId()))             // sub: 사용자 PK(Id)
                .setExpiration(new Date(now + expMillis)) // 만료시간
                .signWith(secret, SignatureAlgorithm.HS256) // 서명
                .compact(); // JWT 생성
    }
}
