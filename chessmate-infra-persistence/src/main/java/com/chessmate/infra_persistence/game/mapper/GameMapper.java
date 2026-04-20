package com.chessmate.infra_persistence.game.mapper;

import com.chessmate.domain.game.Game;
import com.chessmate.infra_persistence.game.entity.GameJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class GameMapper {

    public GameJpaEntity toEntity(Game domain) {
        return GameJpaEntity.builder()
            .id(domain.getId())
            .userId(domain.getUserId())
            .platform(domain.getPlatform())
            .platformGameId(domain.getPlatformGameId())
            .username(domain.getUsername())
            .opponentUsername(domain.getOpponentUsername())
            .playerColor(domain.getPlayerColor())
            .result(domain.getResult())
            .timeClass(domain.getTimeClass())
            .timeControl(domain.getTimeControl())
            .rated(domain.getRated())
            .rating(domain.getRating())
            .moves(domain.getMoves())
            .variant(domain.getVariant())
            .playedAt(domain.getPlayedAt())
            .createdAt(domain.getCreatedAt())
            .build();
    }

    public Game toDomain(GameJpaEntity entity) {
        return Game.builder()
            .id(entity.getId())
            .userId(entity.getUserId())
            .platform(entity.getPlatform())
            .platformGameId(entity.getPlatformGameId())
            .username(entity.getUsername())
            .opponentUsername(entity.getOpponentUsername())
            .playerColor(entity.getPlayerColor())
            .result(entity.getResult())
            .timeClass(entity.getTimeClass())
            .timeControl(entity.getTimeControl())
            .rated(entity.getRated())
            .rating(entity.getRating())
            .moves(entity.getMoves())
            .variant(entity.getVariant())
            .playedAt(entity.getPlayedAt())
            .createdAt(entity.getCreatedAt())
            .build();
    }
}