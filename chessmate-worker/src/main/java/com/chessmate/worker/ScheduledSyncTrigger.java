package com.chessmate.worker;

import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.domain.chesscom.user.ChesscomUserRepository;
import com.chessmate.domain.lichess.user.LichessUserRepository;
import com.chessmate.domain.sync.SyncJob;
import com.chessmate.domain.sync.SyncJobRepository;
import com.chessmate.domain.sync.SyncStatus;
import com.chessmate.infra_redis.sync.SyncJobProducer;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 4시간 간격 전체 사용자 증분 수집 스케쥴러.
 *
 * 동작 방식:
 * 1. DB에서 전체 Lichess / Chess.com 사용자 목록 조회
 * 2. 각 사용자에 대해 SyncJob 생성 후 Redis 큐에 enqueue
 * 3. SyncJobDispatcher가 큐를 폴링해 Workers에 전달 (기존 파이프라인 재사용)
 *
 * [Lichess 증분]
 * - SyncJob은 cursor 없이 생성
 * - LichessGameSyncWorker.process()가 DB의 max(played_at)을 읽어
 *   since 파라미터로 신규 게임만 요청 (API 레벨 필터링)
 *
 * [Chess.com 증분]
 * - ScheduledSyncTrigger가 cursor를 전월(yyyy/MM)로 사전 설정
 * - ChessComGameSyncWorker.filterPending()이 당월 아카이브만 반환
 * - saveAll 내부 중복 체크로 이미 저장된 게임 자동 스킵
 *
 * [최초 사용자 처리]
 * - Lichess: 게임 없으면 Worker가 자동으로 전체 수집 모드 진입
 * - Chess.com: 이전 COMPLETED 이력 없으면 cursor=null → 전체 아카이브 수집
 *
 * Rate Limit 참고:
 * - Lichess: RateLimiter(0.05 req/s) 내장 → 대량 사용자도 안전
 * - Chess.com: RateLimiter(0.5~2 req/s) 내장 → archive 단위 순차/병렬 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledSyncTrigger {

    private static final DateTimeFormatter YEAR_MONTH_FMT = DateTimeFormatter.ofPattern("yyyy/MM");

    private final LichessUserRepository lichessUserRepository;
    private final ChesscomUserRepository chesscomUserRepository;
    private final SyncJobRepository syncJobRepository;
    private final SyncJobProducer syncJobProducer;

    /**
     * 4시간마다 전체 사용자 증분 수집 트리거.
     * cron: 0시 / 4시 / 8시 / 12시 / 16시 / 20시 정각 실행
     */
    @Scheduled(cron = "0 0 */4 * * *")
    public void triggerAllUsers() {
        log.info("[ScheduledSyncTrigger] 정기 증분 수집 시작");
        int lichessCount = scheduleLichessUsers();
        int chesscomCount = scheduleChesscomUsers();
        log.info("[ScheduledSyncTrigger] 정기 증분 수집 enqueue 완료 — Lichess={}명, Chess.com={}명",
            lichessCount, chesscomCount);
    }

    /**
     * 전체 Lichess 사용자 SyncJob enqueue.
     * Worker가 DB 상태를 보고 전체/증분 수집 모드를 자동 결정.
     */
    private int scheduleLichessUsers() {
        var users = lichessUserRepository.findAll();
        for (var user : users) {
            try {
                SyncJob job = SyncJob.create(user.getId(), OAuthPlatForm.LICHESS, user.getUsername());
                SyncJob saved = syncJobRepository.save(job);
                syncJobProducer.enqueue(OAuthPlatForm.LICHESS, saved.getId());
                log.debug("[ScheduledSyncTrigger] Lichess enqueue userId={} username={}", user.getId(), user.getUsername());
            } catch (Exception e) {
                log.error("[ScheduledSyncTrigger] Lichess enqueue 실패 userId={} error={}", user.getId(), e.getMessage());
            }
        }
        return users.size();
    }

    /**
     * 전체 Chess.com 사용자 SyncJob enqueue.
     *
     * - 이전 COMPLETED 이력 있음: cursor=전월 → 당월 아카이브만 재수집 (증분)
     * - 이전 이력 없거나 FAILED: cursor=null → 전체 아카이브 수집 (초기 수집 재시도)
     *
     * 전월 커서 설정 이유:
     *   Chess.com 아카이브는 월 단위로 확정(immutable)되므로
     *   지난달까지는 이미 DB에 저장됨. 당월 아카이브에만 새 게임이 추가됨.
     *   filterPending("yyyy/MM") → 해당 월 이후만 반환 → 당월 archive 1건만 호출.
     */
    private int scheduleChesscomUsers() {
        // 전월을 cursor로 설정 → filterPending이 당월 archive만 반환
        String prevMonthCursor = YearMonth.now().minusMonths(1).format(YEAR_MONTH_FMT);

        var users = chesscomUserRepository.findAll();
        for (var user : users) {
            try {
                Optional<SyncJob> lastJob = syncJobRepository.findLatestByUserIdAndPlatform(
                    user.getId(), OAuthPlatForm.CHESSCOM);

                SyncJob job;
                if (lastJob.isPresent() && lastJob.get().getStatus() == SyncStatus.COMPLETED) {
                    // 증분: 전월 커서 → 당월만 재수집
                    job = SyncJob.createWithCursor(
                        user.getId(), OAuthPlatForm.CHESSCOM, user.getUsername(), prevMonthCursor);
                } else {
                    // 최초 또는 이전 실패: 전체 수집
                    job = SyncJob.create(user.getId(), OAuthPlatForm.CHESSCOM, user.getUsername());
                }

                SyncJob saved = syncJobRepository.save(job);
                syncJobProducer.enqueue(OAuthPlatForm.CHESSCOM, saved.getId());
                log.debug("[ScheduledSyncTrigger] Chess.com enqueue userId={} username={} cursor={}",
                    user.getId(), user.getUsername(), job.getSyncCursor());
            } catch (Exception e) {
                log.error("[ScheduledSyncTrigger] Chess.com enqueue 실패 userId={} error={}", user.getId(), e.getMessage());
            }
        }
        return users.size();
    }
}