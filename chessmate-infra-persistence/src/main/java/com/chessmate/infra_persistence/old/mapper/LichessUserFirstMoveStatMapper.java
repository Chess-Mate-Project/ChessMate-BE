//package com.chessmate.infra_persistence.mapper;
//
//import com.chessmate.domain.lichess.userFirstMoveStat.LichessUserFirstMoveStat;
//import com.chessmate.infra_persistence.entity.LichessUserFirstMoveStatEntity;
//import org.springframework.stereotype.Component;
//
///**
// * Lichess 사용자 첫 수 통계 도메인 <-> Entity 매퍼
// * - 각 오프닝별 첫 수 성적 데이터 매핑
// */
//@Component
//public class LichessUserFirstMoveStatMapper {
//
//  /**
//   * LichessUserFirstMoveStat 도메인 모델을 LichessUserFirstMoveStatEntity로 변환
//   *
//   * @param firstMoveStat Lichess 사용자 첫 수 통계 도메인 모델
//   * @return 변환된 LichessUserFirstMoveStatEntity
//   */
//  public LichessUserFirstMoveStatEntity toEntity(LichessUserFirstMoveStat firstMoveStat) {
//    if (firstMoveStat == null) {
//      return null;
//    }
//
//    return LichessUserFirstMoveStatEntity.builder()
//        .id(firstMoveStat.getId())
//        .userPerfId(firstMoveStat.getUserPerfId())
//        .move(firstMoveStat.getMove())
//        .win(firstMoveStat.getWin())
//        .loss(firstMoveStat.getLoss())
//        .draw(firstMoveStat.getDraw())
//        .updatedAt(firstMoveStat.getUpdatedAt())
//        .build();
//  }
//
//  /**
//   * LichessUserFirstMoveStatEntity를 LichessUserFirstMoveStat 도메인 모델로 변환
//   *
//   * @param entity JPA 엔티티
//   * @return 변환된 Lichess 사용자 첫 수 통계 도메인 모델
//   */
//  public LichessUserFirstMoveStat toDomain(LichessUserFirstMoveStatEntity entity) {
//    if (entity == null) {
//      return null;
//    }
//
//    return LichessUserFirstMoveStat.builder()
//        .id(entity.getId())
//        .userId(entity.getUserId())
//        .firstMove(entity.getFirstMove())
//        .color(entity.getColor())
//        .gameType(entity.getGameType())
//        .build();
//  }
//}
//
