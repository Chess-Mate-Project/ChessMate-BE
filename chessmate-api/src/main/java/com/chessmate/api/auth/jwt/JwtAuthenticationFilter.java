    package com.chessmate.api.auth.jwt;

    import com.chessmate.common.code.AuthErrorCode;
    import com.chessmate.common.response.ErrorResponse;
    import jakarta.servlet.FilterChain;
    import jakarta.servlet.ServletException;
    import jakarta.servlet.http.HttpServletRequest;
    import jakarta.servlet.http.HttpServletResponse;
    import java.io.IOException;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.security.core.context.SecurityContextHolder;
    import org.springframework.stereotype.Component;
    import org.springframework.util.StringUtils;
    import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
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
            "/api/oauth/chesscom",

            "/api/oauth/callback",
            "/api/auth/refresh",
            "/api/user/count",
            "/login/oauth/code/chesscom",
            "/oauth2/authorization/lichess"
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
      private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
          return bearerToken.substring(7);
        }
        return null;
      }

    private void setErrorResponse(HttpServletResponse response, AuthErrorCode errorCode) throws IOException {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 설정
      response.setContentType("application/json;charset=UTF-8");

      ErrorResponse errorResponse = new ErrorResponse(errorCode.getStatusCode
          (), errorCode.getMessage());
      String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(errorResponse);

      response.getWriter().write(json);
    }


      @Override
        public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
            String uri = request.getRequestURI();
            log.info("[JWT Filter] 요청 URI: {}", uri);

            String accessToken = resolveToken(request);
            log.debug("[JWT Filter] 엑세스 토큰: {}", accessToken);

            // 토큰이 없거나 빈 경우
            if (accessToken == null || accessToken.isBlank()) {
                log.warn("[JWT Filter] 엑세스 토큰 없음 - uri={}", uri);
                setErrorResponse(response, AuthErrorCode.JWT_TOKEN_NOT_FOUND);
                return;
            }

            // 토큰 검증
            log.info("[JWT Filter] 토큰 검증 시작 - uri={}", uri);
            if (jwtService.validateAccessToken(accessToken)) {
                SecurityContextHolder.getContext().setAuthentication(
                        jwtService.getAuthentication(accessToken)
                );
                log.info("[JWT Filter] 엑세스 토큰 검증 성공, 인증 객체 설정 완료 - uri={}", uri);
                chain.doFilter(request, response);
                return;
            }

            // 토큰 검증 실패
            log.warn("[JWT Filter] 엑세스 토큰 검증 실패 - uri={}", uri);
            setErrorResponse(response, AuthErrorCode.JWT_TOKEN_NOT_FOUND);
        }
    }

