//package com.chessmate.domain.lichess.userPerf;
//
//import com.chessmate.common.type.GameType;
//import java.util.List;
//import java.util.Optional;
//
///**
// * Lichess 사용자 게임 타입별 성적 Repository 인터페이스
// * - 사용자의 각 게임 타입별 성적 데이터 접근 계약 정의
// * - 구현체는 infra_persistence에서 제공
// */
//public interface LichessUserPerfRepository {
//
//  /**
//   * 사용자ID와 게임타입으로 성적 조회
//   *
//   * @param userId 사용자 ID
//   * @param gameType 게임 타입
//   * @return Optional<LichessUserPerf> 성적 도메인 객체
//   */
//  Optional<LichessUserPerf> findByUserIdAndGameType(Long userId, GameType gameType);
//
//  /**
//   * 사용자ID로 모든 성적 조회
//   *
//   * @param userId 사용자 ID
//   * @return 해당 사용자의 모든 게임 타입별 성적 목록
//   */
//  List<LichessUserPerf> findByUserId(Long userId);
//
//  /**
//   * 성적 저장 또는 업데이트
//   *
//   * @param perfStat 저장할 사용자 성적 도메인 객체
//   * @return 저장된 성적 도메인 객체
//   */
//  LichessUserPerf save(LichessUserPerf perfStat);
//
//  /**
//   * 게임 타입별로 랭킹 조회
//   *
//   * @param gameType 게임 타입
//   * @return 해당 게임 타입의 랭킹 상위 사용자 성적 목록
//   */
//  List<LichessUserPerf> findRankingByGameType(GameType gameType);
//
//  /**
//   * 특정 레이팅보다 높은 사용자 수 조회
//   *
//   * @param gameType 게임 타입
//   * @param rating 기준 레이팅
//   * @return 해당 레이팅보다 높은 사용자 수
//   */
//  int countUsersBetterRating(GameType gameType, int rating);
//
//  /**
//   * 사용자ID로 모든 성적 데이터 삭제
//   *
//   * @param userId 사용자 ID
//   */
//  void deleteAllByUserId(Long userId);
//}
//
