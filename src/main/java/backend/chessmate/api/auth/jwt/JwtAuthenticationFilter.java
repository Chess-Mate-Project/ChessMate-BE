    package backend.chessmate.api.auth.jwt;

    import backend.chessmate.api.oauth.repository.UserRepository;
    import jakarta.servlet.FilterChain;
    import jakarta.servlet.ServletException;
    import jakarta.servlet.http.HttpServletRequest;
    import jakarta.servlet.http.HttpServletResponse;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.security.core.context.SecurityContextHolder;
    import org.springframework.web.filter.OncePerRequestFilter;

    import java.io.IOException;

    @Slf4j
    @Configuration
    @RequiredArgsConstructor
    public class JwtAuthenticationFilter extends OncePerRequestFilter {
        private final JwtService jwtService;
        private final UserRepository userRepository;

      @Override
      protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String uri = request.getRequestURI();
        if (uri == null) return true;

        // 필터에서 제외할 경로들 (필요시 추가)
        String[] excluded = {
            "/api/oauth/oauth-url",
            "/api/oauth/callback",
            "/api/oauth/refresh",
            "/api/user/count"
        };

        for (String path : excluded) {
          if (uri.startsWith(path)) {
            return true; // 이 경로들은 필터를 적용하지 않음
          }
        }

        return false; // 그 외는 필터 적용
      }


      @Override
        public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
            String uri = request.getRequestURI();//요청 경로 추출하기
            log.info("요청 URI: {}", uri);



            String accessToken = jwtService.resolveToken(request, JwtRule.ACCESS_PREFIX); // 쿠키에서 엑세스 토큰 추출
            if (accessToken == null || accessToken.isBlank()) {
                log.debug("요청에 액세스 토큰 없음: {}", uri);
                chain.doFilter(request, response);
                return;
            }

            if (jwtService.validateAccessToken(accessToken)) { //엑세스 토큰 검증
                SecurityContextHolder.getContext().setAuthentication( // 엑세스 토큰으로 인증 객체 설정
                        jwtService.getAuthentication(accessToken)
                );
                log.info("엑세스 토큰 검증 성공, 인증 객체 설정 완료");
                chain.doFilter(request, response);
                return;
            }


            chain.doFilter(request, response);



        }
    }

