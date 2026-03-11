//package com.chessmate.infra_persistence.mapper;
//
//import com.chessmate.domain.lichess.userColorStat.LichessUserColorStat;
//import com.chessmate.infra_persistence.entity.LichessUserColorStatEntity;
//import org.springframework.stereotype.Component;
//
///**
// * Lichess 사용자 색상별 통계 도메인 <-> Entity 매퍼
// * - 백(White), 흑(Black) 게임 성적 데이터 매핑
// */
//@Component
//public class LichessUserColorStatMapper {
//
//  /**
//   * LichessUserColorStat 도메인 모델을 LichessUserColorStatEntity로 변환
//   *
//   * @param colorStat Lichess 사용자 색상별 통계 도메인 모델
//   * @return 변환된 LichessUserColorStatEntity
//   */
//  public LichessUserColorStatEntity toEntity(LichessUserColorStat colorStat) {
//    if (colorStat == null) {
//      return null;
//    }
//
//    return LichessUserColorStatEntity.builder()
//        .id(colorStat.getId())
//        .userPerfId(colorStat.getUserPerfId())
//        .color(colorStat.getColor())
//        .win(colorStat.getWin())
//        .loss(colorStat.getLoss())
//        .draw(colorStat.getDraw())
//        .updatedAt(colorStat.getUpdatedAt())
//        .build();
//  }
//
//  /**
//   * LichessUserColorStatEntity를 LichessUserColorStat 도메인 모델로 변환
//   *
//   * @param entity JPA 엔티티
//   * @return 변환된 Lichess 사용자 색상별 통계 도메인 모델
//   */
//  public LichessUserColorStat toDomain(LichessUserColorStatEntity entity) {
//    if (entity == null) {
//      return null;
//    }
//
//    return LichessUserColorStat.builder()
//        .id(entity.getId())
//        .userPerfId(entity.getUserPerfId())
//        .color(entity.getColor())
//        .win(entity.getWin())
//        .loss(entity.getLoss())
//        .draw(entity.getDraw())
//        .updatedAt(entity.getUpdatedAt())
//        .build();
//  }
//}
//
