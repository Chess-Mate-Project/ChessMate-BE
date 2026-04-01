//package com.chessmate.infra_persistence.mapper;
//
//import com.chessmate.domain.lichess.userDailyStreak.LichessUserDailyStreak;
//import com.chessmate.infra_persistence.entity.LichessUserDailyStreakEntity;
//import org.springframework.stereotype.Component;
//
///**
// * Lichess 사용자 일일 스트릭 도메인 <-> Entity 매퍼
// * - 일자별 게임 통계 데이터 매핑
// */
//@Component
//public class LichessUserDailyStreakMapper {
//
//  /**
//   * LichessUserDailyStreak 도메인 모델을 LichessUserDailyStreakEntity로 변환
//   *
//   * @param dailyStreak Lichess 사용자 일일 스트릭 도메인 모델
//   * @return 변환된 LichessUserDailyStreakEntity
//   */
//  public LichessUserDailyStreakEntity toEntity(LichessUserDailyStreak dailyStreak) {
//    if (dailyStreak == null) {
//      return null;
//    }
//
//    return LichessUserDailyStreakEntity.builder()
//        .id(dailyStreak.getId())
//        .userId(dailyStreak.getUserId())
//        .date(dailyStreak.getDate())
//        .win(dailyStreak.getWin())
//        .lose(dailyStreak.getLose())
//        .draw(dailyStreak.getDraw())
//        .lastRating(dailyStreak.getLastRating())
//        .lastGameAt(dailyStreak.getLastGameAt())
//        .build();
//  }
//
//  /**
//   * LichessUserDailyStreakEntity를 LichessUserDailyStreak 도메인 모델로 변환
//   *
//   * @param entity JPA 엔티티
//   * @return 변환된 Lichess 사용자 일일 스트릭 도메인 모델
//   */
//  public LichessUserDailyStreak toDomain(LichessUserDailyStreakEntity entity) {
//    if (entity == null) {
//      return null;
//    }
//
//    return LichessUserDailyStreak.builder()
//        .id(entity.getId())
//        .userId(entity.getUserId())
//        .date(entity.getDate())
//        .win(entity.getWin())
//        .lose(entity.getLose())
//        .draw(entity.getDraw())
//        .lastRating(entity.getLastRating())
//        .lastGameAt(entity.getLastGameAt())
//        .build();
//  }
//}
//
