package com.chessmate.api.sync;

import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.common.response.SuccessResponse;
import com.chessmate.domain.sync.SyncJob;
import com.chessmate.domain.sync.SyncJobRepository;
import com.chessmate.domain.sync.SyncStatus;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 게임 동기화 상태 조회 API.
 * 프론트엔드 폴링용 엔드포인트.
 */
@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncStatusController {

    private final SyncJobRepository syncJobRepository;

    /**
     * GET /api/sync/status/{userId}?platform=LICHESS|CHESSCOM
     *
     * @param userId   서비스 사용자 ID
     * @param platform 플랫폼 (기본값: LICHESS)
     * @return 가장 최근 SyncJob 상태
     */
    @GetMapping("/status/{userId}")
    public ResponseEntity<SuccessResponse<SyncStatusResponse>> getSyncStatus(
        @PathVariable Long userId,
        @RequestParam(defaultValue = "LICHESS") OAuthPlatForm platform
    ) {
        Optional<SyncJob> jobOpt = syncJobRepository.findLatestByUserIdAndPlatform(userId, platform);

        SyncStatusResponse response = jobOpt.map(job -> new SyncStatusResponse(
            job.getId(),
            job.getStatus(),
            job.getTotalFetched(),
            job.getSyncCursor(),
            job.getErrorMsg()
        )).orElse(new SyncStatusResponse(null, SyncStatus.PENDING, 0, null, null));

        return ResponseEntity.ok(new SuccessResponse<>("동기화 상태 조회 성공", response));
    }

    public record SyncStatusResponse(
        Long jobId,
        SyncStatus status,
        int totalFetched,
        String syncCursor,
        String errorMsg
    ) {}
}