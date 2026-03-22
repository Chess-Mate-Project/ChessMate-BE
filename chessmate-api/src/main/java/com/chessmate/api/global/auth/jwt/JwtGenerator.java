package com.chessmate.api.global.auth.jwt;

import com.chessmate.api.global.auth.OAuth2Provider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.security.Key;
import java.util.Date;
import org.springframework.stereotype.Component;

@Component
public class JwtGenerator {

    /**
    * AccessToken을 생성하는 메서드
    * - subject에는 사용자 고유 Id를 담음.
    * - claim에는 각각 Provider(Oauth제공자) 와 Provider의 고유 Id를 저장함
    * - 반환은 String Type의 AccessToken
    * **/
    public String generateAccessToken(Key secret, long expMillis, Long id, OAuth2Provider provider, String providerId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(String.valueOf(id))
                .claim("provider", provider)
                .claim("providerId" , providerId)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expMillis))
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
        return Jwts.builder()
                .setSubject(String.valueOf(id))
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expMillis))
                .signWith(secret, SignatureAlgorithm.HS256)
                .compact();
    }
}
