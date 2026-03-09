package com.chessmate.api.auth.jwt;


import static com.chessmate.api.auth.jwt.JwtRule.ACCESS_PREFIX;
import static com.chessmate.api.auth.jwt.JwtRule.JWT_ISSUE_HEADER;
import static com.chessmate.api.auth.jwt.JwtRule.REFRESH_PREFIX;

import com.chessmate.api.lichess.oauth.OAuth2PrincipalDetails;
import com.chessmate.common.code.AuthErrorCode;
import com.chessmate.common.exception.AuthException;

import com.chessmate.infra_core.entity.Profile;
import com.chessmate.infra_redis.redis.RedisService;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.Key;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JwtService {
    private final JwtGenerator generator;
    private final JwtUtil util;
    private final RedisService redisService;

    private final Key ACCESS_KEY;
    private final Key REFRESH_KEY;
    private final long ACCESS_EXP;
    private final long REFRESH_EXP;
    @Value("${spring.data.redis.key.refresh_token_base}")
    private String REFRESH_TOKEN_KEY;
    @Value("${cookie.domain}")
    private String cookieDomain;

    // 생성자: 의존성 및 JWT 관련 설정값 주입
    public JwtService(
            JwtGenerator jwtGenerator,
            JwtUtil jwtUtil,
            RedisService redisService,
            @Value("${spring.jwt.access-token.secret}") String accessSecret,
            @Value("${spring.jwt.refresh-token.secret}") String refreshSecret,
            @Value("${spring.jwt.access-token.expiration}") long accessExpiration,
            @Value("${spring.jwt.refresh-token.expiration}") long refreshExpiration
    ) {

        this.generator = jwtGenerator;
        this.util = jwtUtil;
        this.redisService = redisService;
        this.ACCESS_KEY = jwtUtil.getSigningKey(accessSecret);
        this.REFRESH_KEY = jwtUtil.getSigningKey(refreshSecret);
        this.ACCESS_EXP = accessExpiration;
        this.REFRESH_EXP = refreshExpiration;
    }

    // 1) Access Token 발급
    @Transactional
    public String generateAccessToken(HttpServletResponse res, Profile profile) {
        String accessToken = generator.generateAccessToken(ACCESS_KEY, ACCESS_EXP, profile);

        ResponseCookie cookie = ResponseCookie.from(ACCESS_PREFIX.getValue(), accessToken)
                .path("/")
                .domain(cookieDomain)
                .httpOnly(false)
                .secure(true)
                .sameSite("Lax")
                .maxAge(60) // 프론트 단에서 바로 읽고 저장소 저장 후 삭제할것이기때문에 짧은 maxAge설정하기.
                .build();
        res.addHeader(JWT_ISSUE_HEADER.getValue(), cookie.toString());

        return accessToken;
    }



    // 2) Refresh Token 발급 + Redis 저장(RTR)
    @Transactional
    public String generateRefreshToken(HttpServletResponse res, Profile profile) {
        String refreshToken = generator.generateRefreshToken(REFRESH_KEY, REFRESH_EXP, profile);

        ResponseCookie cookie = ResponseCookie.from(REFRESH_PREFIX.getValue(), refreshToken)
                .path("/")
                .domain(cookieDomain)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .maxAge(REFRESH_EXP / 1000)
                .build();
        res.addHeader(JWT_ISSUE_HEADER.getValue(), cookie.toString());

        redisService.save(REFRESH_TOKEN_KEY + profile.getId(), refreshToken, REFRESH_EXP);
        return refreshToken;
    }

    // 3) Access Token 검증
    public boolean validateAccessToken(String t) {
        boolean result = util.getTokenStatus(t, ACCESS_KEY) == TokenStatus.AUTHENTICATED;
        return result;
    }

    // 4) Refresh Token 검증 (서명 + Redis 일치 여부)
    public boolean validateRefreshToken(String t, Long identifier) {
        boolean ok = util.getTokenStatus(t, REFRESH_KEY) == TokenStatus.AUTHENTICATED;
        if (!ok) return false;

        String key = REFRESH_TOKEN_KEY + identifier;
        String stored = redisService.get(key, String.class);
        return t.equals(stored);
    }

    // 5) Authentication 객체 생성
    public Authentication getAuthentication(String token) {
        String userId = Jwts.parserBuilder()
                .setSigningKey(ACCESS_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();

    }

    // 6) 쿠키에서 토큰 꺼내기
    public String resolveToken(HttpServletRequest req, JwtRule p) {
        jakarta.servlet.http.Cookie[] cs = req.getCookies();
        if (cs == null) throw new AuthException(AuthErrorCode.JWT_TOKEN_NOT_FOUND);
        return util.resolveTokenFromCookie(cs, p);
    }

    // 7) 로그아웃 처리: 쿠키 만료
    @Transactional
    public void logout(HttpServletResponse res) {
        res.addCookie(util.resetToken(ACCESS_PREFIX));
        res.addCookie(util.resetToken(REFRESH_PREFIX));
    }


  public String getSubject(String refreshToken) {
    return Jwts.parserBuilder()
        .setSigningKey(REFRESH_KEY)
        .build()
        .parseClaimsJws(refreshToken)
        .getBody()
        .getSubject();
  }

}
