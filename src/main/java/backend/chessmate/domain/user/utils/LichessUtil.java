package backend.chessmate.domain.user.utils;

import backend.chessmate.domain.auth.dto.request.OAuthValueRequest;
import backend.chessmate.domain.auth.dto.response.OAuthAccessTokenResponse;

import backend.chessmate.domain.user.dto.api.UserAccount;
import backend.chessmate.domain.auth.entity.User;

import backend.chessmate.global.common.code.ApiErrorCode;
import backend.chessmate.global.common.code.AuthErrorCode;
import backend.chessmate.global.common.code.UserErrorCode;
import backend.chessmate.global.common.exception.ApiException;
import backend.chessmate.global.common.exception.AuthException;
import backend.chessmate.global.common.exception.UserException;
import backend.chessmate.global.config.redis.RedisService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class LichessUtil {

    private final RedisService redisService;

    @Value("${lichess.client-id}")
    private String clientId;

    @Value("${lichess.redirect-url}")
    private String redirectUrl;

    @Value("${lichess.base-url}")
    private String BASE_URL;

    @Value("${spring.data.redis.key.oauth_key_base}")
    private String OAUTH_KEY;

    private final ObjectMapper mapper;


    public OAuthAccessTokenResponse getOAuthAccessToken(OAuthValueRequest request) {
        WebClient webClient = WebClient.builder()
                .baseUrl(BASE_URL)
                .build();

        String requestUri = BASE_URL + "/api/token"
                + "?grant_type=authorization_code"
                + "&code=" + request.getCode()
                + "&client_id=" + clientId
                + "&redirect_uri=" + redirectUrl
                + "&code_verifier=" + request.getCodeVerifier();
        log.info("OAuth 토큰 요청 경로: {}", requestUri);

        return webClient.post()
                .uri("/api/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters
                        .fromFormData("grant_type", "authorization_code")
                        .with("code", request.getCode())
                        .with("client_id", clientId)
                        .with("redirect_uri", redirectUrl)
                        .with("code_verifier", request.getCodeVerifier()))
                .retrieve()

                .onStatus(status -> status.is4xxClientError(), clientResponse -> {
                    return clientResponse.bodyToMono(String.class)
                            .doOnNext(errorBody -> log.error("❌ 4xx 오류 응답 바디: {}", errorBody))
                            .then(Mono.error(new AuthException(AuthErrorCode.FAILD_GET_OAUTH_ACCESS_TOKEN)));
                })


                .bodyToMono(OAuthAccessTokenResponse.class)
                .block();
    }


    public UserAccount getUserAccount(String token) {

        WebClient webClient = WebClient.builder()
                .baseUrl(BASE_URL)
                .build();

        return webClient.get()
                .uri("/api/account")
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .bodyToMono(UserAccount.class)
                .doOnError(e -> {
                    throw new UserException(UserErrorCode.FAILD_GET_USER_ACCOUNT);
                }).block();

    }


    public UserStatsDto getUserGamesApi(User u, Long since, Long until) {

        Map<LocalDate, Status> statusByDate = new HashMap<>();
        Map<String, Long> countByOpening = new HashMap<>();
        Map<String, Long> countByFirstMove = new HashMap<>();

        String oauthKey = OAUTH_KEY + ":" + u.getId();
        String oauthToken = redisService.get(oauthKey, String.class);
        if (oauthToken == null) {
            throw new UserException(UserErrorCode.NOT_FOUND_USER_OAUTH);
        }

        WebClient wc = WebClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + oauthToken)
                .defaultHeader(HttpHeaders.ACCEPT, "application/x-ndjson")
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create().responseTimeout(Duration.ofSeconds(60)))) // 타임아웃 설정
                .build();

        wc.get()
                .uri(ub -> ub
                        .path("/api/games/user/{username}")
                        .queryParam("since", since)
                        .queryParam("until", until)
                        .queryParam("opening", "true")
                        .queryParam("moves", "true")
                        .queryParam("perfType", "bullet,blitz,rapid,classical")
                        .build("chansoo1123")) // 임시로 데이터 많은 유저로 설정 //u.getName();
                .retrieve()
                .onStatus(s -> s.is4xxClientError(), res -> { // 에러 제어하기
                    if (res.statusCode().value() == 429) { // api limit (api 호출 제한)
                        log.error("Lichess API 호출 제한에 걸렸습니다. 잠시 후 다시 시도해주세요.");
                        return Mono.error(new ApiException(ApiErrorCode.API_RATE_LIMIT));
                    } else if (res.statusCode().value() == 401) { // 토큰이 유효하지 않을 떄
                        log.error("Lichess OAuth 토큰이 유효하지 않습니다. 다시 로그인 해주세요.");
                        return Mono.error(new UserException(UserErrorCode.NOT_FOUND_USER_OAUTH));
                    } else { // 그 외 4xx 에러
                        return res.bodyToMono(String.class)
                                .doOnNext(errorBody -> log.error("initUserGamesStreaks Method Error : {}", errorBody))
                                .then(Mono.error(new UserException(UserErrorCode.FAILD_GET_USER_GAMES)));
                    }
                })
                .bodyToFlux(String.class) // NDJSON 형식의 각 라인을 String으로 처리
                .buffer(100) // 100개씩 배치 처리
                .doOnNext(batch -> {
                    log.info("처리된 게임 라인 수: {}", batch.size());
                    for (String line : batch) {
                        aggregateLine(statusByDate, countByOpening, countByFirstMove, line, u);
                    }
                }) // 배치 처리된 각 라인에 대해 aggregateLine 메서드 호출
                .retryWhen(Retry.backoff(5, Duration.ofSeconds(60))
                        .maxBackoff(Duration.ofMinutes(5))
                        .jitter(0.5)
                        .filter(ex -> (ex instanceof ApiException) || (ex instanceof IOException) || (ex instanceof TimeoutException)))
                .blockLast(Duration.ofMinutes(15)); // 모든 배치 처리가 완료될 때까지 대기 (타임아웃 시간은 일단 15분으로 지정함)

        return new UserStatsDto(statusByDate, countByOpening, countByFirstMove);

    }

    public void aggregateLine(
            Map<LocalDate, Status> statusByDate,
            Map<String, Long> countByOpening,
            Map<String, Long> countByFirstMove,
            String line,
            User u
    ) {
        try {
            JsonNode node = mapper.readTree(line);
            long createdAt = node.path("createdAt").asLong();
            long lastMoveAt = node.path("lastMoveAt").asLong();

            LocalDate date = Instant.ofEpochMilli(createdAt)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            String status = node.path("status").asText();  // mate / stalemate / draw
            String winnerColor = node.path("winner").asText(null); // black / white / null(게임 ㅄ됨)

            String winnerName = "Unknown";
            if ("black".equals(winnerColor)) {
                winnerName = node.at("/players/black/user/name").asText("Unknown");
            } else if ("white".equals(winnerColor)) {
                winnerName = node.at("/players/white/user/name").asText("Unknown");
            }


            int win = 0;
            int lose = 0;
            int draw = 0;
            if ("draw".equals(status) || "stalemate".equals(status)) { //무승부 처리
                draw++;
            } else if ("mate".equals(status) || "outoftime".equals(status) || "resign".equals(status)) {
                if (winnerName.equals("chansoo1123")) {//chansoo1123 -> u.userName()로 변경 예정
                    win++;

                } else if (!winnerName.equals("chansoo1123")) { //chansoo1123 -> u.userName()로 변경 예정
                    lose++;
                } else if (winnerName.isEmpty()) {
                    draw++;
                }
            }


            statusByDate
                    .computeIfAbsent(date, d -> new Status())
                    .add(win, lose, draw, lastMoveAt);

            String o = node.at("/opening/name").asText("Unknown");
            if (!o.equals("Unknown") && o.contains(":")) {
                o = StringUtils.substringBefore(o, ":"); // 오프닝 필드 형식 ~~: ~~ 예) Vienna Game: Stanley Variation, Three Knights Variation 여기서 앞부분만 가져오기
            }

            countByOpening.merge(o, 1L, Long::sum);

            String fm = node.at("/moves").asText("Unknown");
            if (!fm.equals("Unknown") && !fm.isEmpty()) {
                fm = fm.split(" ")[0];
            }

            countByFirstMove.merge(fm, 1L, Long::sum);

        } catch (Exception e) {
            log.error("init 파싱 실패 {}", e.getMessage());
            throw new RuntimeException("init 파싱 실패 error", e);
        }


    }

//    public Map<LocalDate, Status> updateUserGameStreaks(User u, long since, long until) {
//
//        String oauthKey = OAUTH_KEY + ":" + u.getId();
//        String oauthToken = redisService.get(oauthKey, String.class);
//
//        WebClient wc = WebClient.builder()
//                .baseUrl(BASE_URL)
//                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + oauthToken)
//                .defaultHeader(HttpHeaders.ACCEPT, "application/x-ndjson")
//                .build();
//
//        List<String> lines = wc.get()
//                .uri(ub -> ub
//                        .path("/api/games/user/{username}")
//                        .queryParam("since", since)
//                        .queryParam("until", until)
//                        .queryParam("opening", "true")
//                        .queryParam("perfType", "bullet,blitz,rapid,classical")
//                        .build("chansoo1123"))//u.getName();
//                .retrieve()
//                .bodyToFlux(String.class)
//                .retryWhen(Retry.backoff(5, Duration.ofSeconds(2)))
//                .collectList()
//                .block();
//
//        if (lines != null) {
//            for (String line : lines) {
//                aggregateLine(statusByDate, line, u);
//            }
//        }
//
//        return statusByDate;
//    }


}





