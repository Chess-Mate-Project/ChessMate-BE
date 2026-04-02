package com.chessmate.api.global.auth.jwt;

import com.chessmate.common.dto.OAuthPlatForm;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.security.Key;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtGenerator {

    /**
    * AccessToken을 생성하는 메서드
    * - subject에는 사용자 고유 Id를 담음.
    * - claim에는 각각 Provider(Oauth제공자) 와 Provider의 고유 Id를 저장함
    * - 반환은 String Type의 AccessToken
    * **/
    public String generateAccessToken(Key secret, long expMillis, Long id, OAuthPlatForm provider, String providerId) {
        long now = System.currentTimeMillis();
        long expiration = now + expMillis;

        log.info("AccessToken 생성 - ID: {}, Provider: {}, 만료시간: {}ms ({}초), 현재시간: {}ms",
            id, provider, expMillis, expMillis / 1000, now);

        return Jwts.builder()
                .setSubject(String.valueOf(id))
                .claim("provider", provider)
                .claim("providerId" , providerId)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(expiration))
                .signWith(secret, SignatureAlgorithm.HS256)
                .compact();
    }

  /**
   * RefreshToken을 생성하는 메서드
   * - subject에는 사용자 고유 Id를 담음.
   * - 반환은 String Type의 RefreshToken
   * **/
    public String generateRefreshToken(Key secret, long expMillis, Long id) {
        long now = System.currentTimeMillis();
        long expiration = now + expMillis;

        log.info("RefreshToken 생성 - ID: {}, 만료시간: {}ms ({}초), 현재시간: {}ms",
            id, expMillis, expMillis / 1000, now);

        return Jwts.builder()
                .setSubject(String.valueOf(id))
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(expiration))
                .signWith(secret, SignatureAlgorithm.HS256)
                .compact();
    }
}
