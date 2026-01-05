package backend.chessmate.api.oauth.service;


import static backend.chessmate.api.external.util.LichessUtil.generateCodeChallenge;
import static backend.chessmate.api.external.util.LichessUtil.generateRandomCodeVerifier;
import static backend.chessmate.api.external.util.LichessUtil.generateRandomState;

import backend.chessmate.api.external.config.LichessConfig;
import backend.chessmate.api.external.dto.account.LichessAccountDto;
import backend.chessmate.api.external.service.LichessApiService;
import backend.chessmate.api.auth.jwt.JwtService;
import backend.chessmate.api.oauth.dto.OauthAccessTokenDto;
import backend.chessmate.api.oauth.dto.request.OAuthValueRequest;
import backend.chessmate.api.oauth.dto.response.OauthUrlResponse;
import backend.chessmate.api.user.entity.User;
import backend.chessmate.api.oauth.repository.UserRepository;
import backend.chessmate.global.CacheService;
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

    /*
    * - [ Lichess OAuth Callback 처리 ]
    * - 설명: Lichess OAuth 서버로부터 전달받은 Authorization Code와
    *  State를 처리하여 OAuth Access Token을 발급받고,
    *  사용자 정보를 조회하여 데이터베이스에 저장하고, 변동성이 있는 데이터는 캐싱
    * */
    public void callback(String code, String state, HttpServletResponse res) {

      // State에 대응하는 Code Verifier 조회
      String codeVerifier = cacheService.getPkce(state);

      // Code Verifier가 없으면 예외 발생
      if (codeVerifier == null) {
        throw new IllegalStateException("Invalid or expired OAuth state");
      }

      // State-코드 검증 후 캐시에서 삭제
      cacheService.deletePkce(state);

      // OAuth Access Token 발급 요청
      log.info("code: " + code);
      log.info("codeVerifier: " + codeVerifier);
      OauthAccessTokenDto dto = lichessApiService.getOAuthAccessToken(
          new OAuthValueRequest(code, codeVerifier)
      );
      log.info("accessToken: " + dto.accessToken());
      log.info("tokenType: " + dto.tokenType());
      log.info("expiresIn: " + dto.expiresIn());

      // 사용자 정보 조회
      LichessAccountDto accountDto = lichessApiService.getUserAccount(dto.accessToken());
      // 신규 사용자면 DB에 저장
      if (!userRepository.existsByLichessId(accountDto.id())) {

        User newUser = User.builder()
            .lichessId(accountDto.id())
            .username(accountDto.username())
            .build();

        userRepository.save(newUser);
      }

      // 변동성이 있는 데이터 캐싱
      var playTime = accountDto.playTime();
      var count = accountDto.count();
      var perfs = accountDto.perfs();
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


