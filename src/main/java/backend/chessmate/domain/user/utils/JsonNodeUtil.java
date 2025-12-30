package backend.chessmate.domain.user.utils;

import backend.chessmate.domain.user.dto.*;
import backend.chessmate.domain.user.dto.history.TierPointDto;
import backend.chessmate.domain.user.dto.mapper.UserBasicMapper;
import backend.chessmate.domain.user.dto.mapper.UserProfileMapper;
import backend.chessmate.domain.user.dto.mapper.UserRatingHistoryMapper;
import backend.chessmate.domain.user.entity.type.GameType;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

public class JsonNodeUtil {

    public static UserBasicMapper mapToUserBasicInfo(JsonNode jsonNode) {
        String lichessId = jsonNode.get("id").asText();
        String name = jsonNode.get("username").asText();
        long createdAtMillis = jsonNode.path("createdAt").asLong(0L); //밀리초
        LocalDate createdAt = LocalDate.ofInstant(
                Instant.ofEpochMilli(createdAtMillis),
                ZoneId.systemDefault()
        );

        return UserBasicMapper.builder()
                .lichessId(lichessId)
                .name(name)
                .createdAt(createdAt)
                .build();
    }

    public static UserPlayCountDto mapToUserPlayCountDto(JsonNode jsonNode) {
        int all = jsonNode.path("count").path("all").asInt(0);
        int win = jsonNode.path("count").path("win").asInt(0);
        int lose = jsonNode.path("count").path("loss").asInt(0);
        int draw = jsonNode.path("count").path("draw").asInt(0);

        return UserPlayCountDto.builder()
                .all(all)
                .win(win)
                .lose(lose)
                .draw(draw)
                .build();
    }

    public static GameSummaryDto mapToGameSummaryDto(JsonNode jsonNode) {
        int classical = jsonNode.path("perfs").path("classical").path("games").asInt(0);
        int rapid = jsonNode.path("perfs").path("rapid").path("games").asInt(0);
        int bullet = jsonNode.path("perfs").path("bullet").path("games").asInt(0);
        int blitz = jsonNode.path("perfs").path("blitz").path("games").asInt(0);

        return GameSummaryDto.builder()
                .classical(classical)
                .rapid(rapid)
                .bullet(bullet)
                .blitz(blitz)
                .build();
    }

    public static UserProfileMapper mapToUserProfileDto(JsonNode jsonNode) {
        long playSeconds = jsonNode.path("playTime").path("total").asLong(0L); //초

        String bio = jsonNode.get("profile").path("bio").asText(null);
        String flag = jsonNode.get("profile").path("flag").asText(null);
        String link = jsonNode.get("profile").path("links").asText(null);

        return UserProfileMapper.builder()
                .playTime(playSeconds)
                .bio(bio)
                .flag(flag)
                .link(link)
                .build();
    }

    public static UserRatingByGameTypesMapper mapToUserRatingByGameTypesDto(JsonNode jsonNode) {
        int classical = jsonNode.path("perfs").path("classical").path("rating").asInt(0);
        int rapid = jsonNode.path("perfs").path("rapid").path("rating").asInt(0);
        int bullet = jsonNode.path("perfs").path("bullet").path("rating").asInt(0);
        int blitz = jsonNode.path("perfs").path("blitz").path("rating").asInt(0);

        return UserRatingByGameTypesMapper.builder()
                .classicalRating(classical)
                .rapidRating(rapid)
                .bulletRating(bullet)
                .blitzRating(blitz)
                .build();
    }

    public static UserRatingHistoryMapper mapToUserRatingHistoryByTierHistoryDto(JsonNode jsonNode) {
        UserRatingHistoryMapper mapper = new UserRatingHistoryMapper();

        for (JsonNode type : jsonNode) {
            List<TierPointDto> points = new ArrayList<>();

            for (JsonNode pointNode : type.path("points")) {
                int year = pointNode.get(0).asInt();
                int month = pointNode.get(1).asInt() + 1;
                int day = pointNode.get(2).asInt();
                int rating = pointNode.get(3).asInt();

                LocalDate date = LocalDate.of(year, month, day);
                TierResult tierResult = TierUtil.calculateTier(rating);

                TierPointDto tierPointDto = TierPointDto.builder()
                        .date(date)
                        .tier(tierResult)
                        .build();

                points.add(tierPointDto);
            }
            switch (type.get("name").asText()) {
                case "Classical" -> mapper.setClassicalHistory(points);

                case "Rapid" -> mapper.setRapidHistory(points);

                case "Bullet" -> mapper.setBulletHistory(points);

                case "Blitz" -> mapper.setBlitzHistory(points);
            }

        }
        return mapper;
    }
}
