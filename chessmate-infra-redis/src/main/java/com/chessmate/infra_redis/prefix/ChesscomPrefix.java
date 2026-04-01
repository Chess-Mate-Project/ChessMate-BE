package com.chessmate.infra_redis.prefix;
import lombok.Getter;

@Getter
public enum ChesscomPrefix {

  // Refresh Token 접두사: "chesscom:refreshtoken:123" 형태로 저장
  REFRESH_TOKEN("chesscom:refreshtoken:"),

  // Access Token 접두사: "chesscom:accesstoken:123" 형태로 저장
  ACCESS_TOKEN("chesscom:accesstoken:");

  private final String prefix;

  ChesscomPrefix(String prefix) {
    this.prefix = prefix;
  }

  /**
   * providerId를 결합하여 Redis에서 사용할 최종 Key를 생성합니다.
   * @param providerId 정수형 ID
   * @return 완성된 Redis Key 문자열
   */
  public String createKey(Long providerId) {
    return this.prefix + providerId;
  }
}
