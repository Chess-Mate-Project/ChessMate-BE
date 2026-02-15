-- UserPerf 중복 데이터 방지를 위한 DB 마이그레이션 스크립트
-- 작성일: 2026-02-14
-- 목적: user_perfs 테이블에서 (user_id, game_type) 조합의 중복 생성 원천 차단

-- 1. 현재 상태 확인
SELECT
    user_id,
    COUNT(*) as row_count
FROM user_perfs
GROUP BY user_id
HAVING COUNT(*) >= 5
ORDER BY row_count DESC;

-- 2. 중복 데이터 제거 (가장 최신 ID만 유지)
-- 주의: 이미 삭제 완료된 상태라면 스킵
/*
DELETE FROM user_perfs
WHERE id NOT IN (
    SELECT id FROM (
        SELECT MAX(id) AS id
        FROM user_perfs
        GROUP BY user_id, game_type
    ) AS tmp
);
*/

-- 3. Unique 제약 조건 추가 (동시성 제어의 마지막 방어선)
-- 이 제약 조건으로 동일한 user_id와 game_type 조합은 1개만 유지 가능
ALTER TABLE user_perfs
ADD CONSTRAINT uk_user_perf_user_gametype
UNIQUE (user_id, game_type);

-- 4. 제약 조건 확인
SELECT CONSTRAINT_NAME, TABLE_NAME, COLUMN_NAME
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE TABLE_NAME = 'user_perfs'
AND CONSTRAINT_NAME LIKE '%uk_%';

-- 5. 최종 검증: 각 user별로 정확히 4개(BULLET, BLITZ, RAPID, CLASSICAL)만 존재하는지 확인
SELECT
    user_id,
    COUNT(*) as total_count,
    SUM(CASE WHEN game_type = 'BULLET' THEN 1 ELSE 0 END) as bullet_count,
    SUM(CASE WHEN game_type = 'BLITZ' THEN 1 ELSE 0 END) as blitz_count,
    SUM(CASE WHEN game_type = 'RAPID' THEN 1 ELSE 0 END) as rapid_count,
    SUM(CASE WHEN game_type = 'CLASSICAL' THEN 1 ELSE 0 END) as classical_count
FROM user_perfs
GROUP BY user_id
HAVING total_count != 4 OR bullet_count != 1 OR blitz_count != 1 OR rapid_count != 1 OR classical_count != 1
ORDER BY user_id;
-- 결과가 없으면 정상 상태입니다.

