package com.chessmate.api.global.auth.jwt;

import com.chessmate.api.global.auth.oauth.common.CookieName;
import com.chessmate.common.dto.OAuthPlatForm;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;

    private String resolveTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            log.debug("요청에 쿠키가 없습니다");
            return null;
        }

        String token = Arrays.stream(request.getCookies())
            .filter(cookie -> {
                String cookieName = cookie.getName();
                return Arrays.stream(OAuthPlatForm.values())
                    .anyMatch(platform -> cookieName.equals(CookieName.ACCESS_TOKEN.of(platform)));
            })
            .map(cookie -> {
                log.debug("ACCESS_TOKEN 쿠키 발견: {}", cookie.getName());
                return cookie.getValue();
            })
            .findFirst()
            .orElse(null);

        if (token == null) {
            log.debug("ACCESS_TOKEN 쿠키를 찾을 수 없습니다");
            log.debug("현재 쿠키: {}",
                Arrays.stream(request.getCookies())
                    .map(c -> c.getName() + "=" + c.getValue().substring(0, Math.min(20, c.getValue().length())) + "...")
                    .collect(java.util.stream.Collectors.joining(", ")));
        }

        return token;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String accessToken = resolveTokenFromCookie(request);

        if (StringUtils.hasText(accessToken)) {
            try {
                if (jwtService.validateAccessToken(accessToken)) {
                    Authentication authentication = jwtService.getAuthentication(accessToken);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("JWT 토큰으로 인증 성공: {}", authentication.getName());
                } else {
                    log.warn("JWT 토큰 검증 실패 - validateAccessToken 반환값이 false");
                }
            } catch (ExpiredJwtException e) {
                log.warn("JWT 토큰 만료: {}", e.getMessage());
            } catch (JwtException e) {
                log.warn("JWT 토큰 무효: {}", e.getMessage());
            } catch (Exception e) {
                log.error("JWT 토큰 처리 중 예상치 못한 에러 발생", e);
            }
        } else {
            log.debug("요청에서 JWT 토큰을 찾을 수 없음. 경로: {}", request.getRequestURI());
        }

        chain.doFilter(request, response);
    }
}



