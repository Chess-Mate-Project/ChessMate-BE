package backend.chessmate.api.auth.jwt;

import backend.chessmate.api.user.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Map;

@Component
public class JwtGenerator {

    // 공통 헤더
    private static final Map<String, Object> HEADER = Map.of(
            "typ", "JWT",
            "alg", "HS256"
    );

    // 1) Access Token 생성
    public String generateAccessToken(Key secret, long expMillis, User user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setHeader(HEADER)
                .setSubject(String.valueOf(user.getId()))      // sub: 사용자 PK(Id)
                .setExpiration(new Date(now + expMillis))       // 만료시간
                .signWith(secret, SignatureAlgorithm.HS256)     // 서명
                .compact(); // JWT 생성
    }

    // 2) Refresh Token 생성
    public String generateRefreshToken(Key secret, long expMillis, User user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setHeader(HEADER)
                .setSubject(String.valueOf(user.getId()))             // sub: 사용자 PK(Id)
                .setExpiration(new Date(now + expMillis)) // 만료시간
                .signWith(secret, SignatureAlgorithm.HS256) // 서명
                .compact(); // JWT 생성
    }
}
