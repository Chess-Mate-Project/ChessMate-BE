package com.chessmate.infra_redis.prefix;


import lombok.Getter;

@Getter
public enum LichessPrefix {

  // Lichess Access Token 접두사: "lichess:accesstoken:123" 형태로 저장
  ACCESS_TOKEN("lichess:accesstoken:");

  private final String prefix;

  LichessPrefix(String prefix) {
    this.prefix = prefix;
  }

  /**
   * providerId를 결합하여 Redis에서 사용할 최종 Key를 생성합니다.
   * @param providerId 정수형 ID
   * @return 완성된 Redis Key 문자열
   */
  public String createKey(String providerId) {
    return this.prefix + providerId;
  }
}
