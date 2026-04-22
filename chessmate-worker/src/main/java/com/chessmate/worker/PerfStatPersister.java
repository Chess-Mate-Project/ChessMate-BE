package com.chessmate.worker;

import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.domain.stat.UserPerfStat;
import com.chessmate.domain.stat.UserPerfStatRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PerfStatPersister {

    private final UserPerfStatRepository perfStatRepository;

    @Transactional
    public void replaceStats(Long userId, OAuthPlatForm platform, List<UserPerfStat> stats) {
        perfStatRepository.deleteByUserIdAndPlatform(userId, platform);
        perfStatRepository.saveAll(stats);
    }
}
