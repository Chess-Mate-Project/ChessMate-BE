    package com.chessmate.api.auth.jwt;

    import jakarta.servlet.FilterChain;
    import jakarta.servlet.ServletException;
    import jakarta.servlet.http.HttpServletRequest;
    import jakarta.servlet.http.HttpServletResponse;
    import java.io.IOException;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.security.core.context.SecurityContextHolder;
    import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;

      @Override
      protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) return true;

        // 필터에서 제외할 경로들 - 인증이 필요 없는 공개 엔드포인트만 추가
        // /api/rank/ranking은 인증된 사용자와 비인증 사용자 모두 접근 가능하므로 필터 적용 필요
        String[] excluded = {
            "/api/oauth/oauth-url",
            "/api/oauth/callback",
            "/api/auth/refresh",
            "/api/user/count"
        };

        for (String path : excluded) {
          if (uri.startsWith(path)) {
            log.debug("[Filter Skip] 필터 제외 경로 - uri={}", uri);
            return true;
          }
        }

        if (request.getMethod().equals("OPTIONS")) {
          log.debug("[Filter Skip] OPTIONS 요청 - uri={}", uri);
          return true;
        }
        return false;
      }


      @Override
        public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
            String uri = request.getRequestURI();
            log.info("[JWT Filter] 요청 URI: {}", uri);

            String accessToken = jwtService.resolveToken(request, JwtRule.ACCESS_PREFIX);
            if (accessToken == null || accessToken.isBlank()) {
                log.info("[JWT Filter] 엑세스 토큰 없음 - 비인증 사용자로 처리: {}", uri);
                chain.doFilter(request, response);
                return;
            }

            log.info("[JWT Filter] 토큰 검증 시작 - uri={}", uri);
            if (jwtService.validateAccessToken(accessToken)) {
                SecurityContextHolder.getContext().setAuthentication(
                        jwtService.getAuthentication(accessToken)
                );
                log.info("[JWT Filter] 엑세스 토큰 검증 성공, 인증 객체 설정 완료 - uri={}", uri);
                chain.doFilter(request, response);
                return;
            }

            log.warn("[JWT Filter] 엑세스 토큰 검증 실패 - uri={}", uri);
            chain.doFilter(request, response);



        }
    }

