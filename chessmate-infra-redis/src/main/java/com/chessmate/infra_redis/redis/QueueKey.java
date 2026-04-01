package com.chessmate.infra_redis.redis;

/**
 * Redis Queue 키를 관리하는 Enum
 * 모든 Queue 키를 중앙화하여 관리합니다.
 */
public enum QueueKey {
  /**
   * 게임 동기화 작업 큐
   * 사용자의 모든 게임 데이터를 수집하고 처리하는 비동기 작업
   */
  GAME_SYNC_TASKS("queue:game-sync-tasks"),

  /**
   * 사용자 통계 업데이트 큐
   * 게임 결과 기반으로 사용자 통계를 갱신하는 작업
   */
  USER_STATS_UPDATE("queue:user-stats-update"),

  /**
   * 사용자 프로필 동기화 큐
   * 플랫폼에서 사용자 프로필 정보를 수집하는 작업
   */
  USER_PROFILE_SYNC("queue:user-profile-sync");

  private final String key;

  QueueKey(String key) {
    this.key = key;
  }

  /**
   * Queue 키를 반환합니다.
   *
   * @return Redis에서 사용할 Queue 키
   */
  public String getKey() {
    return key;
  }

  /**
   * Queue 이름을 반환합니다.
   *
   * @return Queue의 논리적 이름
   */
  public String getName() {
    return this.name();
  }
}

