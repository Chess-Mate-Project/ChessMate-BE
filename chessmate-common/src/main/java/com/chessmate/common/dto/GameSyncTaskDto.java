package com.chessmate.common.dto;

/**
 * Worker 서버로 전달할 게임 동기화 작업 정보
 *
 * Producer에서 사용자 회원가입 시 Redis Queue에 이 객체를 넣어서
 * Worker 서버가 게임 데이터를 가져와 통계를 처리하도록 지시합니다.
 */
public class GameSyncTaskDto {

  /**
   * 게임 데이터를 동기화할 플랫폼
   * 예: "LICHESS", "CHESSCOM"
   */
  private OAuthPlatForm platform;

  /**
   * 플랫폼에서의 사용자명
   * 게임 데이터 조회 시 사용합니다.
   */
  private String platformUsername;

  /**
   * 우리 서비스의 사용자 ID
   */
  private Long serviceUserId;

  /**
   * 작업 생성 시간 (타임스탬프)
   */
  private Long createdAt;

  /**
   * 작업 우선순위
   * 1: 높음 (신규 가입자, 우선 처리)
   * 5: 일반 (정기적 업데이트)
   */
  private Integer priority;

  /**
   * 작업의 고유한 ID
   * 중복 방지 및 추적 용도로 사용됩니다.
   */
  private String taskId;

  /**
   * 재시도 횟수
   * 작업 실패 시 재시도 횟수를 추적합니다.
   */
  private Integer retryCount;

  // ======================== 생성자 ========================

  public GameSyncTaskDto() {
    this.priority = 1;
    this.retryCount = 0;
  }

  public GameSyncTaskDto(OAuthPlatForm platform, String platformUsername, Long serviceUserId, Long createdAt) {
    this.platform = platform;
    this.platformUsername = platformUsername;
    this.serviceUserId = serviceUserId;
    this.createdAt = createdAt;
    this.priority = 1;
    this.retryCount = 0;
  }

  public GameSyncTaskDto(OAuthPlatForm platform, String platformUsername, Long serviceUserId, Long createdAt, Integer priority) {
    this.platform = platform;
    this.platformUsername = platformUsername;
    this.serviceUserId = serviceUserId;
    this.createdAt = createdAt;
    this.priority = priority;
    this.retryCount = 0;
  }

  // ======================== Getter ========================

  public OAuthPlatForm getPlatform() {
    return platform;
  }

  public String getPlatformUsername() {
    return platformUsername;
  }

  public Long getServiceUserId() {
    return serviceUserId;
  }

  public Long getCreatedAt() {
    return createdAt;
  }

  public Integer getPriority() {
    return priority;
  }

  public String getTaskId() {
    return taskId;
  }

  public Integer getRetryCount() {
    return retryCount;
  }

  // ======================== Setter ========================

  public void setPlatform(OAuthPlatForm platform) {
    this.platform = platform;
  }

  public void setPlatformUsername(String platformUsername) {
    this.platformUsername = platformUsername;
  }

  public void setServiceUserId(Long serviceUserId) {
    this.serviceUserId = serviceUserId;
  }

  public void setCreatedAt(Long createdAt) {
    this.createdAt = createdAt;
  }

  public void setPriority(Integer priority) {
    this.priority = priority;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  public void setRetryCount(Integer retryCount) {
    this.retryCount = retryCount;
  }

  // ======================== Builder ========================

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private OAuthPlatForm platform;
    private String platformUsername;
    private Long serviceUserId;
    private Long createdAt;
    private Integer priority = 1;
    private String taskId;
    private Integer retryCount = 0;

    public Builder platform(OAuthPlatForm platform) {
      this.platform = platform;
      return this;
    }

    public Builder platformUsername(String platformUsername) {
      this.platformUsername = platformUsername;
      return this;
    }

    public Builder serviceUserId(Long serviceUserId) {
      this.serviceUserId = serviceUserId;
      return this;
    }

    public Builder createdAt(Long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder priority(Integer priority) {
      this.priority = priority;
      return this;
    }

    public Builder taskId(String taskId) {
      this.taskId = taskId;
      return this;
    }

    public Builder retryCount(Integer retryCount) {
      this.retryCount = retryCount;
      return this;
    }

    public GameSyncTaskDto build() {
      GameSyncTaskDto dto = new GameSyncTaskDto();
      dto.platform = this.platform;
      dto.platformUsername = this.platformUsername;
      dto.serviceUserId = this.serviceUserId;
      dto.createdAt = this.createdAt;
      dto.priority = this.priority;
      dto.taskId = this.taskId;
      dto.retryCount = this.retryCount;
      return dto;
    }
  }

  // ======================== toString ========================

  @Override
  public String toString() {
    return "GameSyncTaskDto{" +
        "platform=" + platform +
        ", platformUsername='" + platformUsername + '\'' +
        ", serviceUserId=" + serviceUserId +
        ", createdAt=" + createdAt +
        ", priority=" + priority +
        ", taskId='" + taskId + '\'' +
        ", retryCount=" + retryCount +
        '}';
  }
}

