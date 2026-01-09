package com.chessmate.api.oauth.service;


import static com.chessmate.external.util.LichessUtil.generateCodeChallenge;
import static com.chessmate.external.util.LichessUtil.generateRandomCodeVerifier;
import static com.chessmate.external.util.LichessUtil.generateRandomState;

import com.chessmate.api.auth.jwt.JwtService;
import com.chessmate.api.oauth.dto.OauthUrlResponse;
import com.chessmate.common.service.UserBatchService;
import com.chessmate.domain.user.User;
import com.chessmate.domain.user.UserRepository;
import com.chessmate.external.config.LichessConfig;
import com.chessmate.external.dto.account.LichessAccountDto;
import com.chessmate.external.dto.account.PerfsDto;
import com.chessmate.external.dto.account.PlayTimeDto;
import com.chessmate.external.dto.account.UserCountDto;
import com.chessmate.external.dto.oauth.OAuthValueRequest;
import com.chessmate.external.dto.oauth.OauthAccessTokenDto;
import com.chessmate.external.service.LichessApiService;
import com.chessmate.infra_redis.redis.CacheService;
import com.chessmate.infra_redis.redis.RedisService;
import com.chessmate.redis.UserEventProducer;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class OauthService {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final LichessConfig lichessConfig;

    private final LichessApiService lichessApiService;
    private final CacheService cacheService;
    private final UserEventProducer userEventProducer;

  /**
     * - [ Lichess OAuth URL 생성 ]
     * - 설명: Lichess OAuth 인증을 위한 URL을 생성하여 반환
     * - @return OauthUrlResponse Lichess OAuth 인증 URL
     * */
    public OauthUrlResponse getOauthUrl() {
      String code_verifier = generateRandomCodeVerifier();


      String code_challenge_method = "S256";
        String code_challenge = generateCodeChallenge(code_verifier);
        String response_type = "code";
        String client_id = lichessConfig.getClientId();
        String redirect_uri = lichessConfig.getRedirectUrl();
        String scope = "email:read";
        String state = generateRandomState();

      cacheService.savePkce(state, code_verifier);



      Map<String,String> parameters = Map.of(
          "code_challenge_method", code_challenge_method,
          "code_challenge", code_challenge,
          "response_type", response_type,
          "client_id", client_id,
          "redirect_uri", redirect_uri,
          "scope", scope,
          "state", state
      );


      log.info(code_verifier + " <- code_verifier 저장 완료");
      String paramString = parameters.entrySet().stream()
          .map(kv -> kv.getKey() + "=" + kv.getValue())
          .collect(Collectors.joining("&"));

      URI frontChannelUrl = URI.create(lichessConfig.getOauthUrl()+ "?" + paramString);

      log.info(frontChannelUrl.toString());
      return new OauthUrlResponse(frontChannelUrl.toString());


    }

    /**
    * - [ Lichess OAuth Callback 처리 ]
    * - 설명: Lichess OAuth 서버로부터 전달받은 Authorization Code와
    *  State를 처리하여 OAuth Access Token을 발급받고,
    *  사용자 정보를 조회하여 데이터베이스에 저장하고, 변동성이 있는 데이터는 캐싱
    * */
    public void   callback(String code, String state, HttpServletResponse res) {

      // State에 대응하는 Code Verifier 조회
      String codeVerifier = cacheService.getPkce(state);

      // Code Verifier가 없으면 예외 발생
      if (codeVerifier == null) {
        throw new IllegalStateException("Invalid or expired OAuth state");
      }

      // State-코드 검증 후 캐시에서 삭제
      cacheService.deletePkce(state);

      // OAuth Access Token 발급 요청
      OauthAccessTokenDto dto = lichessApiService.getOAuthAccessToken(
          new OAuthValueRequest(code, codeVerifier)
      );

      // 사용자 정보 조회
      LichessAccountDto accountDto = lichessApiService.getUserAccount(dto.accessToken());
      // 신규 사용자면 DB에 저장
      boolean isUser = userRepository.existsByLichessId(accountDto.id());
      if (!isUser) {

        User newUser = User.builder()
            .lichessId(accountDto.id())
            .username(accountDto.username())
            .title(accountDto.title())
            .build();

        userRepository.save(newUser);

        // 배치 작업 트리거 (첫 로그인 사용자만 정보 전체 조회)
        userEventProducer.publishUserCreated(newUser.getId(), dto.accessToken());
        log.info("배치 작업 트리거 시작 - OauthService / + " + newUser.getId() + "//" + dto.accessToken());
      }

      // 변동성이 있는 데이터 캐싱
      PlayTimeDto playTime = accountDto.playTime();
      UserCountDto count = accountDto.count();
      PerfsDto perfs = accountDto.perfs();
      var lichessId = accountDto.id();

      cacheService.saveLichessToken(lichessId, dto.accessToken());
      cacheService.savePlayTime(lichessId, playTime);
      cacheService.savePerfs(lichessId, perfs);
      cacheService.saveUserCount(lichessId, count);

      // JWT 토큰 발급
      User user = userRepository.findByLichessId(lichessId)
          .orElseThrow(() -> new IllegalStateException("User not found"));
      jwtService.generateRefreshToken(res, user);
      var refreshToken = jwtService.generateAccessToken(res, user);

      cacheService.saveRefreshToken(user.getId(), refreshToken);
    }



}


