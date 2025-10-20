package backend.chessmate.domain.auth.service;


import backend.chessmate.domain.auth.config.jwt.JwtService;
import backend.chessmate.domain.auth.dto.request.OAuthValueRequest;
import backend.chessmate.domain.auth.dto.response.OAuthAccessTokenResponse;
import backend.chessmate.domain.auth.entity.Role;
import backend.chessmate.domain.auth.entity.User;
import backend.chessmate.domain.auth.repository.UserRepository;
import backend.chessmate.domain.user.dto.UserBasicMapper;
import backend.chessmate.domain.user.service.StatService;
import backend.chessmate.domain.user.utils.JsonNodeUtil;
import backend.chessmate.global.config.redis.RedisService;
import backend.chessmate.domain.user.utils.LichessUtil;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final RedisService redisService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final LichessUtil lichessutil;
    private final StatService statService;

    @Value("${spring.data.redis.key.oauth_key_base}")
    private String REDIS_OAUTH_KEY;

    @Value("${spring.data.redis.key.refresh_token_base}")
    private String REFRESH_TOKEN_KEY;

    @Value("${spring.jwt.refresh-token.expiration}")
    private long REFRESH_TOKEN_EXPIRATION;


    public void login(OAuthValueRequest request, HttpServletResponse res) {

        OAuthAccessTokenResponse oauthTokenResponse = lichessutil.getOAuthAccessToken(request);
        String oauthToken = oauthTokenResponse.getAccessToken();


        //우선 UserAccount 호출 후 LichessId로 레디스 키 구성

        JsonNode userAccount = lichessutil.getUserAccount(oauthToken);
        UserBasicMapper userBasicInfo = JsonNodeUtil.mapToUserBasicInfo(userAccount);

        Optional<User> userOptional = userRepository.findByLichessId(userBasicInfo.getLichessId());
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            log.info("이미 존재하는 사용자입니다. Lichess ID: {}", userBasicInfo.getLichessId());
            String accessToken = jwtService.generateAccessToken(res, user);
            String refreshToken = jwtService.generateRefreshToken(res, user);
            String oauthKey = REDIS_OAUTH_KEY + ":" + user.getId();
            String refreshKey = REFRESH_TOKEN_KEY + ":" + user.getId();
            redisService.save(oauthKey, oauthToken, oauthTokenResponse.getExpiresIn());
            redisService.save(refreshKey, refreshToken, REFRESH_TOKEN_EXPIRATION);

            return;
        }

        User newUser = User.builder()
                .lichessId(userBasicInfo.getLichessId())
                .name(userBasicInfo.getName())
                .createdAt(userBasicInfo.getCreatedAt())
                .role(Role.USER)
                .build();
        userRepository.save(newUser);


        String accessToken = jwtService.generateAccessToken(res, newUser);
        String refreshToken = jwtService.generateRefreshToken(res, newUser);

        String oauthKey = REDIS_OAUTH_KEY + ":" + newUser.getId();
        String refreshKey = REFRESH_TOKEN_KEY + ":" + newUser.getId();
        // 유저의 OAuthAccessToken을 Redis에 저장
        redisService.save(oauthKey, oauthToken, oauthTokenResponse.getExpiresIn());
        redisService.save(refreshKey, refreshToken, REFRESH_TOKEN_EXPIRATION);

        statService.initGames(newUser);
    }

    public void logout(User user, HttpServletResponse res) {
        long id = user.getId();
        String oauthKey = REDIS_OAUTH_KEY + ":" + id;
        String refreshKey = REFRESH_TOKEN_KEY + ":" + id;

        // Redis에서 OAuth 토큰 삭제
        redisService.delete(oauthKey);
        redisService.delete(refreshKey);

        // JWT 쿠키 삭제
        jwtService.logout(res);
    }


}
