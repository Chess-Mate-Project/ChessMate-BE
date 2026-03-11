//package com.chessmate.infra_persistence.repositoryImpl;
//
//import com.chessmate.domain.lichess.userPerf.LichessUserPerf;
//import com.chessmate.domain.lichess.userPerf.LichessUserPerfRepository;
//import com.chessmate.infra_persistence.jpaRepository.LichessUserPerfJpaRepository;
//import com.chessmate.infra_persistence.mapper.LichessUserPerfMapper;
//import com.chessmate.common.type.GameType;
//import java.util.List;
//import java.util.Optional;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Repository;
//
///**
// * Lichess 사용자 게임 타입별 성적 Repository 구현체
// * - 도메인 인터페이스를 JPA를 통해 구현
// * - 데이터베이스 접근 로직을 캡슐화
// */
//@Slf4j
//@Repository
//@RequiredArgsConstructor
//public class LichessUserPerfRepositoryImpl implements LichessUserPerfRepository {
//
//  private final LichessUserPerfJpaRepository jpaRepository;
//  private final LichessUserPerfMapper mapper;
//
//  /**
//   * 사용자ID와 게임타입으로 성적 조회
//   *
//   * @param userId 사용자 ID
//   * @param gameType 게임 타입
//   * @return Optional<LichessUserPerf> 성적 도메인 객체
//   */
//  @Override
//  public Optional<LichessUserPerf> findByUserIdAndGameType(Long userId, GameType gameType) {
//    log.debug("[LichessUserPerfRepository] 사용자 성적 조회: userId={}, gameType={}", userId, gameType);
//    return jpaRepository.findByUserIdAndGameType(userId, gameType)
//        .map(mapper::toDomain);
//  }
//
//  /**
//   * 사용자ID로 모든 성적 조회
//   *
//   * @param userId 사용자 ID
//   * @return 해당 사용자의 모든 게임 타입별 성적 목록
//   */
//  @Override
//  public List<LichessUserPerf> findByUserId(Long userId) {
//    log.debug("[LichessUserPerfRepository] 사용자 모든 성적 조회: userId={}", userId);
//    return jpaRepository.findByUserId(userId)
//        .stream()
//        .map(mapper::toDomain)
//        .toList();
//  }
//
//  /**
//   * 성적 저장 또는 업데이트
//   *
//   * @param perfStat 저장할 사용자 성적 도메인 객체
//   * @return 저장된 성적 도메인 객체
//   */
//  @Override
//  public LichessUserPerf save(LichessUserPerf perfStat) {
//    log.debug("[LichessUserPerfRepository] 성적 저장: userId={}, gameType={}", perfStat.getUserId(), perfStat.getGameType());
//    var entity = mapper.toEntity(perfStat);
//    var savedEntity = jpaRepository.save(entity);
//    return mapper.toDomain(savedEntity);
//  }
//
//  /**
//   * 게임 타입별로 랭킹 조회
//   *
//   * @param gameType 게임 타입
//   * @return 해당 게임 타입의 랭킹 상위 사용자 성적 목록
//   */
//  @Override
//  public List<LichessUserPerf> findRankingByGameType(GameType gameType) {
//    log.debug("[LichessUserPerfRepository] 랭킹 조회: gameType={}", gameType);
//    return jpaRepository.findRankingByGameType(gameType)
//        .stream()
//        .map(mapper::toDomain)
//        .toList();
//  }
//
//  /**
//   * 특정 레이팅보다 높은 사용자 수 조회
//   *
//   * @param gameType 게임 타입
//   * @param rating 기준 레이팅
//   * @return 해당 레이팅보다 높은 사용자 수
//   */
//  @Override
//  public int countUsersBetterRating(GameType gameType, int rating) {
//    log.debug("[LichessUserPerfRepository] 상위 사용자 수 조회: gameType={}, rating={}", gameType, rating);
//    return jpaRepository.countUsersBetterRating(gameType, rating);
//  }
//
//  /**
//   * 사용자ID로 모든 성적 데이터 삭제
//   *
//   * @param userId 사용자 ID
//   */
//  @Override
//  public void deleteAllByUserId(Long userId) {
//    log.debug("[LichessUserPerfRepository] 사용자 성적 전체 삭제: userId={}", userId);
//    jpaRepository.deleteAllByUserId(userId);
//  }
//}
//
