-- ============================================================
-- ChessMate-BE Database Schema (MySQL 8.0+)
-- ============================================================

-- ※ 참고
-- OAuthPlatForm  : CHESSCOM | LICHESS
-- SyncStatus     : PENDING | IN_PROGRESS | COMPLETED | FAILED | TOKEN_EXPIRED
-- GameResult     : WIN | LOSS | DRAW | OTHER
-- refresh_token  : Redis 저장 (DB 테이블 없음)
-- rating_history : game 테이블 집계 쿼리로 조회 (별도 테이블 없음)
-- ============================================================


-- ─────────────────────────────────────────
-- 1. lichess_users
-- ─────────────────────────────────────────
CREATE TABLE lichess_users (
    id                 BIGINT          NOT NULL AUTO_INCREMENT,
    lichess_id         VARCHAR(255)    NOT NULL,
    username           VARCHAR(255)    NOT NULL,
    description        VARCHAR(255)    NULL,
    banner_image       VARCHAR(255)    NULL,
    profile_image      VARCHAR(255)    NULL,
    created_at         DATETIME        NOT NULL,
    updated_at         DATETIME        NULL,
    platform_joined_at DATETIME        NULL,
    deleted_at         DATETIME        NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_lichess_id (lichess_id),
    UNIQUE KEY uq_lichess_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ─────────────────────────────────────────
-- 2. chesscom_users
-- ─────────────────────────────────────────
CREATE TABLE chesscom_users (
    id                 BIGINT          NOT NULL AUTO_INCREMENT,
    chesscom_id        BIGINT          NOT NULL,
    username           VARCHAR(255)    NOT NULL,
    description        TEXT            NULL,
    banner             TEXT            NULL,   -- JSON 형태 배너 정보
    profile            TEXT            NULL,   -- JSON 형태 프로필 정보
    created_at         DATETIME        NOT NULL,
    updated_at         DATETIME        NULL,
    platform_joined_at DATETIME        NULL,
    deleted_at         DATETIME        NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_chesscom_id (chesscom_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ─────────────────────────────────────────
-- 3. game
-- ─────────────────────────────────────────
CREATE TABLE game (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NOT NULL,
    platform         VARCHAR(20)  NOT NULL,   -- CHESSCOM | LICHESS
    platform_game_id VARCHAR(255) NOT NULL,
    username         VARCHAR(100) NOT NULL,
    opponent_username VARCHAR(100) NULL,
    player_color     VARCHAR(10)  NULL,        -- white | black
    result           VARCHAR(10)  NULL,        -- WIN | LOSS | DRAW | OTHER
    time_class       VARCHAR(20)  NULL,        -- bullet | blitz | rapid | classical
    time_control     VARCHAR(30)  NULL,        -- 예: 3+2, 600+0
    rated            TINYINT(1)   NULL,
    rating           INT          NULL,
    moves            TEXT         NULL,        -- PGN 기보
    variant          VARCHAR(30)  NULL,        -- standard | chess960 ...
    played_at        DATETIME     NULL,
    created_at       DATETIME     NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_game_platform_id (platform, platform_game_id),
    INDEX idx_game_user_platform (user_id, platform)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ─────────────────────────────────────────
-- 4. sync_job
-- ─────────────────────────────────────────
CREATE TABLE sync_job (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    user_id           BIGINT       NOT NULL,
    platform          VARCHAR(20)  NOT NULL,   -- CHESSCOM | LICHESS
    platform_username VARCHAR(100) NOT NULL,
    status            VARCHAR(20)  NOT NULL,   -- PENDING | IN_PROGRESS | COMPLETED | FAILED | TOKEN_EXPIRED
    sync_cursor       VARCHAR(255) NULL,        -- Lichess: lastGameId / Chess.com: "yyyy/MM"
    total_fetched     INT          NOT NULL DEFAULT 0,
    error_msg         VARCHAR(500) NULL,
    created_at        DATETIME     NOT NULL,
    updated_at        DATETIME     NOT NULL,

    PRIMARY KEY (id),
    INDEX idx_user_platform (user_id, platform),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ─────────────────────────────────────────
-- 5. user_perf_stat
--    타임클래스별 레이팅 / 승·무·패 집계
-- ─────────────────────────────────────────
CREATE TABLE user_perf_stat (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    platform   VARCHAR(20) NOT NULL,   -- CHESSCOM | LICHESS
    time_class VARCHAR(20) NOT NULL,   -- bullet | blitz | rapid | classical
    rating     INT         NOT NULL DEFAULT 0,
    games      INT         NOT NULL DEFAULT 0,
    wins       INT         NOT NULL DEFAULT 0,
    losses     INT         NOT NULL DEFAULT 0,
    draws      INT         NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE KEY uk_perf_stat (user_id, platform, time_class),
    INDEX idx_perf_stat_user_platform (user_id, platform)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ─────────────────────────────────────────
-- 6. user_color_stat
--    색상(백/흑)별 승·무·패 집계
-- ─────────────────────────────────────────
CREATE TABLE user_color_stat (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    platform   VARCHAR(20) NOT NULL,   -- CHESSCOM | LICHESS
    time_class VARCHAR(20) NOT NULL,
    color      VARCHAR(10) NOT NULL,   -- white | black
    wins       INT         NOT NULL DEFAULT 0,
    draws      INT         NOT NULL DEFAULT 0,
    losses     INT         NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE KEY uk_color_stat (user_id, platform, time_class, color),
    INDEX idx_color_stat_user_platform (user_id, platform)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ─────────────────────────────────────────
-- 7. user_daily_game_stat
--    날짜별 게임 수 집계 (스트릭 히트맵용)
-- ─────────────────────────────────────────
CREATE TABLE user_daily_game_stat (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    platform   VARCHAR(20) NOT NULL,   -- CHESSCOM | LICHESS
    date       DATE        NOT NULL,
    total      INT         NOT NULL DEFAULT 0,
    wins       INT         NOT NULL DEFAULT 0,
    draws      INT         NOT NULL DEFAULT 0,
    losses     INT         NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE KEY uk_daily_stat (user_id, platform, date),
    INDEX idx_daily_stat_user_platform (user_id, platform)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ─────────────────────────────────────────
-- 8. user_first_move_stat
--    첫 수(e4 / d4 / c5 ...) 별 사용 횟수
-- ─────────────────────────────────────────
CREATE TABLE user_first_move_stat (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    platform   VARCHAR(20) NOT NULL,   -- CHESSCOM | LICHESS
    time_class VARCHAR(20) NOT NULL,
    color      VARCHAR(10) NOT NULL,   -- white | black
    move       VARCHAR(10) NOT NULL,   -- 예: e2e4, d2d4
    count      INT         NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE KEY uk_first_move_stat (user_id, platform, time_class, color, move),
    INDEX idx_first_move_user_platform (user_id, platform)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;