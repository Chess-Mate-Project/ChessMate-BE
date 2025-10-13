    package backend.chessmate.domain.auth.config.jwt;

    import backend.chessmate.domain.auth.entity.User;
    import backend.chessmate.domain.auth.repository.UserRepository;
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
        public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
            String uri = request.getRequestURI();//요청 경로 추출하기
            log.info("요청 URI: {}", uri);
            if (uri.startsWith("/api/auth/login") ||
                    uri.contains("/swagger-ui") ||
                    uri.startsWith("/v3/api-docs") ||
                    uri.startsWith("/swagger-resources")
            ) {
                chain.doFilter(request, response);
                return;
            }


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

            log.info("엑세스 토큰 만료됨, 리프레시 토큰 검증 시작");

            String refreshToken = jwtService.resolveToken(request, JwtRule.REFRESH_PREFIX); // 쿠키에서 리프레시 토큰 추출
            String id = jwtService.getAuthentication(refreshToken).getName(); //리프레시 토큰으로 인증 객체에서 Id 추출 (getName()이 Id를 반환함)
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다.")); //리프레시 토큰으로 인증 객체에서 Id 추출 후 사용자 조회


            if (jwtService.validateRefreshToken(refreshToken, user.getId())) { //리프레시 토큰 검증
                String newAccessToken = jwtService.generateAccessToken(response, user);// 새로운 엑세스 토큰 발급

                jwtService.generateRefreshToken(response, user); //새로운 리프레쉬 토큰 발급(발급 시 자동 레디스 저장) - RTR(Rotate-Refresh-Token) 이라고 함

                SecurityContextHolder.getContext().setAuthentication(
                        jwtService.getAuthentication(newAccessToken)
                );// 새로운 엑세스 토큰으로 인증 객체 설정

                chain.doFilter(request, response); //다음 필터로 이동
                return;
            }




            jwtService.logout(response);
            chain.doFilter(request, response);



        }
    }

