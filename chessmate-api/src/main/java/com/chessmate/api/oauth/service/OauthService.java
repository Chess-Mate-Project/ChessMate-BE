package com.chessmate.api.oauth.service;


import static com.chessmate.external.util.LichessUtil.generateCodeChallenge;
import static com.chessmate.external.util.LichessUtil.generateRandomCodeVerifier;
import static com.chessmate.external.util.LichessUtil.generateRandomState;

import com.chessmate.api.auth.jwt.JwtService;
import com.chessmate.api.oauth.dto.OauthUrlResponse;
import com.chessmate.api.redis.LichessApiProducer;
import com.chessmate.common.code.UserErrorCode;
import com.chessmate.common.exception.UserException;
import com.chessmate.domain.user.User;
import com.chessmate.domain.user.UserRepository;
import com.chessmate.domain.userPerf.UserPerfRepository;
import com.chessmate.external.config.LichessConfig;
import com.chessmate.external.dto.account.LichessAccountDto;
import com.chessmate.external.dto.oauth.OAuthValueRequest;
import com.chessmate.external.dto.oauth.OauthAccessTokenDto;
import com.chessmate.external.service.LichessApiService;
import com.chessmate.infra_redis.redis.CacheService;
import com.chessmate.infra_redis.redis.dto.TaskType;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
    private final UserPerfRepository userPerfRepository;
    private final LichessConfig lichessConfig;

    private final LichessApiService lichessApiService;
    private final CacheService cacheService;
    private final LichessApiProducer lichessApiProducer;

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
        String state = generateRandomState();

      cacheService.savePkce(state, code_verifier);



      Map<String,String> parameters = Map.of(
          "code_challenge_method", code_challenge_method,
          "code_challenge", code_challenge,
          "response_type", response_type,
          "client_id", client_id,
          "redirect_uri", redirect_uri,
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
            .allGames(accountDto.count().all())
            .ratedGames(accountDto.count().rated())
            .wins(accountDto.count().win())
            .losses(accountDto.count().loss())
            .draws(accountDto.count().draw())
            .totalSeconds(accountDto.playTime().total())
            .lichessCreatedAt(
                LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(accountDto.createdAt()),
                    ZoneId.systemDefault()
                )
            )
            .createdAt(LocalDateTime.now())
            .build();

        userRepository.save(newUser);

        // 저장된 사용자 정보 조회
        User savedUser = userRepository.findByUsername(accountDto.username()).orElseThrow(
            () -> new UserException(UserErrorCode.NOT_FOUND_USER)
        );

//        // 게임 타입별 통계 조회 및 저장 (4개 기본 게임 타입만)
//        GameType[] gameTypes = {GameType.BULLET, GameType.BLITZ, GameType.RAPID, GameType.CLASSICAL};
//
//        for (GameType gameType : gameTypes) {
//          try {
//            log.info("[UserPerf] API 호출 시작 - username={}, gameType={}", savedUser.getUsername(), gameType);
//
//            UserPerfDto userPerfDto = lichessApiService.getUserPerf(savedUser.getUsername(), gameType);
//
//            log.info("[UserPerf] API 응답 수신 - username={}, gameType={}, rating={}",
//                savedUser.getUsername(), gameType, userPerfDto.perf().glicko().rating());
//
//            // 불확실성 계산 (레이티드 게임 50회 미만이면 uncertain = true)
//            int ratedCount = userPerfDto.stat().count().rated();
//            boolean isUncertain = ratedCount < 50;
//
//            // 최고/최저 레이팅 조회 (null 체크)
//            int highestRating = userPerfDto.stat().highest() != null ? userPerfDto.stat().highest().int_() : 0;
//            int lowestRating = userPerfDto.stat().lowest() != null ? userPerfDto.stat().lowest().int_() : 0;
//
//            // 최대 연승/연패 조회 (null 체크)
//            int maxWinStreak = userPerfDto.stat().resultStreak() != null &&
//                              userPerfDto.stat().resultStreak().win() != null &&
//                              userPerfDto.stat().resultStreak().win().max() != null
//                ? userPerfDto.stat().resultStreak().win().max().v() : 0;
//
//            int maxLossStreak = userPerfDto.stat().resultStreak() != null &&
//                               userPerfDto.stat().resultStreak().loss() != null &&
//                               userPerfDto.stat().resultStreak().loss().max() != null
//                ? userPerfDto.stat().resultStreak().loss().max().v() : 0;
//
//            // CountDto에서 게임 통계 조회
//            CountDto countDto = userPerfDto.stat().count();
//
//            log.info("[UserPerf] 통계 데이터 추출 - username={}, gameType={}, all={}, rated={}, wins={}, losses={}, highest={}, lowest={}",
//                savedUser.getUsername(), gameType, countDto.all(), countDto.rated(), countDto.win(), countDto.loss(), highestRating, lowestRating);
//
//            UserPerf userPerf = UserPerf.builder()
//                .userId(savedUser.getId())
//                .gameType(gameType)
//                .rating(userPerfDto.perf().glicko().rating().intValue())
//                .gamesPlayed(userPerfDto.perf().nb())
//                .prov(userPerfDto.perf().glicko().provisional() != null &&
//                      userPerfDto.perf().glicko().provisional())
//                .all(countDto.all())
//                .rated(countDto.rated())
//                .wins(countDto.win())
//                .losses(countDto.loss())
//                .draws(countDto.draw())
//                .tour(countDto.tour())
//                .berserk(countDto.berserk())
//                .opAvg(countDto.opAvg())
//                .seconds(countDto.seconds())
//                .disconnects(countDto.disconnects())
//                .highestRating(highestRating)
//                .lowestRating(lowestRating)
//                .maxStreak(maxWinStreak)
//                .maxLossStreak(maxLossStreak)
//                .uncertain(isUncertain)
//                .build();
//
//            userPerfRepository.save(userPerf);
//            log.info("[UserPerf] DB 저장 완료 - userId={}, gameType={}, rating={}, rated={}, uncertain={}",
//                savedUser.getId(), gameType, userPerf.getRating(), ratedCount, isUncertain);
//
//          } catch (Exception e) {
//            log.error("[UserPerf] API 호출 또는 저장 실패 - username={}, gameType={}, errorMessage={}, errorClass={}",
//                savedUser.getUsername(), gameType, e.getMessage(), e.getClass().getSimpleName(), e);
//          }
//        }

        // Lichess OAuth 토큰 캐싱
        cacheService.saveLichessToken(savedUser.getId(), dto.accessToken());


        lichessApiProducer.sendSyncTask(savedUser, accountDto.username(), dto.accessToken(), TaskType.PERF, true);
        lichessApiProducer.sendSyncTask(savedUser, accountDto.username(), dto.accessToken(), TaskType.GAMES, true);
        log.info("배치 작업 트리거 시작 - OauthService / userId={}, accessToken={}", savedUser.getId(), dto.accessToken());
      }

      String lichessId = accountDto.id();
      // JWT 토큰 발급
      User user = userRepository.findByLichessId(lichessId)
          .orElseThrow(() -> new IllegalStateException("User not found"));
      jwtService.generateRefreshToken(res, user);
      var refreshToken = jwtService.generateAccessToken(res, user);

      cacheService.saveRefreshToken(user.getId(), refreshToken);
    }



}


