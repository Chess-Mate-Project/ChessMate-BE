//package com.chessmate.infra_persistence.mapper;
//
//import com.chessmate.domain.lichess.userPerf.LichessUserPerf;
//import com.chessmate.infra_persistence.entity.LichessUserPerfEntity;
//import org.springframework.stereotype.Component;
//
///**
// * Lichess 사용자 성적 도메인 <-> Entity 매퍼
// * - 도메인 모델과 JPA 엔티티 간의 상호 변환을 담당
// * - 게임 타입별 레이팅 및 통계 데이터 매핑
// */
//@Component
//public class LichessUserPerfMapper {
//
//  /**
//   * LichessUserPerf 도메인 모델을 LichessUserPerfEntity로 변환
//   *
//   * @param perf Lichess 사용자 성적 도메인 모델
//   * @return 변환된 LichessUserPerfEntity
//   */
//  public LichessUserPerfEntity toEntity(LichessUserPerf perf) {
//    if (perf == null) {
//      return null;
//    }
//
//    return LichessUserPerfEntity.builder()
//        .id(perf.getId())
//        .userId(perf.getUserId())
//        .gameType(perf.getGameType())
//        .rating(perf.getRating())
//        .rd(perf.get())
//        .prog(perf.getProg())
//        .ratedGamesCount(perf.getRatedGamesCount())
//        .win(perf.getWin())
//        .loss(perf.getLoss())
//        .draw(perf.getDraw())
//        .bestRating(perf.getBestRating())
//        .bestRatingDate(perf.getBestRatingDate())
//        .updatedAt(perf.getUpdatedAt())
//        .build();
//  }
//
//  /**
//   * LichessUserPerfEntity를 LichessUserPerf 도메인 모델로 변환
//   *
//   * @param entity JPA 엔티티
//   * @return 변환된 Lichess 사용자 성적 도메인 모델
//   */
//  public LichessUserPerf toDomain(LichessUserPerfEntity entity) {
//    if (entity == null) {
//      return null;
//    }
//
//    return LichessUserPerf.builder()
//        .id(entity.getId())
//        .userId(entity.getUserId())
//        .gameType(entity.getGameType())
//        .rating(entity.getRating())
//        .rd(entity.getRd())
//        .prog(entity.getProg())
//        .ratedGamesCount(entity.getRatedGamesCount())
//        .win(entity.getWin())
//        .loss(entity.getLoss())
//        .draw(entity.getDraw())
//        .bestRating(entity.getBestRating())
//        .bestRatingDate(entity.getBestRatingDate())
//        .updatedAt(entity.getUpdatedAt())
//        .build();
//  }
//}
//
