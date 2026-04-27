package com.chessmate.api.stat.service;

import com.chessmate.api.stat.dto.ColorStatResponse;
import com.chessmate.api.stat.dto.DailyGameStatResponse;
import com.chessmate.api.stat.dto.FirstMoveStatResponse;
import com.chessmate.api.stat.dto.MonthlyRatingEntry;
import com.chessmate.api.stat.dto.RatingHistoryResponse;
import com.chessmate.api.stat.dto.StreakResponse;
import com.chessmate.api.stat.dto.UserPerfStatResponse;
import com.chessmate.api.stat.dto.YearlyGameStatResponse;
import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.domain.stat.UserColorStat;
import com.chessmate.domain.stat.UserColorStatRepository;
import com.chessmate.domain.stat.UserDailyGameStatRepository;
import com.chessmate.domain.stat.UserFirstMoveStatRepository;
import com.chessmate.domain.stat.UserMonthlyRatingStatRepository;
import com.chessmate.domain.stat.UserPerfStatRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatService {

    private final UserDailyGameStatRepository dailyStatRepository;
    private final UserColorStatRepository colorStatRepository;
    private final UserFirstMoveStatRepository firstMoveStatRepository;
    private final UserPerfStatRepository perfStatRepository;
    private final UserMonthlyRatingStatRepository monthlyRatingStatRepository;

    private static final DateTimeFormatter YEAR_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    /** 색상별 통계. timeClass가 있으면 해당 타입만 필터 */
    public List<ColorStatResponse> getColorStat(Long userId, OAuthPlatForm platform, String timeClass) {
        List<UserColorStat> stats = timeClass != null
            ? colorStatRepository.findByUserIdAndPlatformAndTimeClass(userId, platform, timeClass)
            : colorStatRepository.findByUserIdAndPlatform(userId, platform);

        return stats.stream().map(ColorStatResponse::from).toList();
    }

    /** 첫 수 통계. timeClass가 있으면 해당 타입만 필터 */
    public List<FirstMoveStatResponse> getFirstMoveStat(Long userId, OAuthPlatForm platform, String timeClass) {
        var stats = timeClass != null
            ? firstMoveStatRepository.findByUserIdAndPlatformAndTimeClass(userId, platform, timeClass)
            : firstMoveStatRepository.findByUserIdAndPlatform(userId, platform);

        return stats.stream().map(FirstMoveStatResponse::from).toList();
    }

    /**
     * 년도별 일별 게임 스트릭.
     * year 없으면 전체 연도 반환, 있으면 해당 연도만.
     * currentStreak: 오늘 또는 어제를 기준으로 연속으로 게임을 한 일수.
     */
    public StreakResponse getStreak(Long userId, OAuthPlatForm platform, Integer year) {
        // currentStreak 계산을 위해 전체 날짜 목록 조회
        List<LocalDate> allDates = dailyStatRepository.findByUserIdAndPlatform(userId, platform)
            .stream()
            .map(stat -> stat.getDate())
            .sorted(Comparator.reverseOrder())
            .toList();

        int currentStreak = calculateCurrentStreak(allDates);

        List<DailyGameStatResponse> filtered = year != null
            ? dailyStatRepository.findByUserIdAndPlatformAndYear(userId, platform, year)
                .stream().map(DailyGameStatResponse::from).toList()
            : dailyStatRepository.findByUserIdAndPlatform(userId, platform)
                .stream().map(DailyGameStatResponse::from).toList();

        List<YearlyGameStatResponse> years = filtered.stream()
            .sorted(Comparator.comparing(DailyGameStatResponse::date))
            .collect(Collectors.groupingBy(d -> d.date().getYear()))
            .entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> new YearlyGameStatResponse(e.getKey(), e.getValue()))
            .toList();

        return new StreakResponse(currentStreak, years);
    }

    /**
     * 오늘(또는 어제)부터 거슬러 올라가며 연속 플레이 일수를 계산한다.
     * - 가장 최근 날짜가 오늘이거나 어제여야 스트릭이 유효하다.
     */
    private int calculateCurrentStreak(List<LocalDate> sortedDatesDesc) {
        if (sortedDatesDesc.isEmpty()) {
            return 0;
        }

        LocalDate today = LocalDate.now();
        LocalDate mostRecent = sortedDatesDesc.get(0);

        // 가장 최근 게임이 오늘도 어제도 아니면 스트릭 종료
        if (mostRecent.isBefore(today.minusDays(1))) {
            return 0;
        }

        int streak = 0;
        LocalDate expected = mostRecent;

        for (LocalDate date : sortedDatesDesc) {
            if (date.equals(expected)) {
                streak++;
                expected = expected.minusDays(1);
            } else if (date.isBefore(expected)) {
                break;
            }
        }

        return streak;
    }

    /**
     * 타임클래스별 레이팅 + 승/무/패 통계 (user_perf_stat).
     * platform으로 조회, timeClass 지정 시 해당 타입 하나만 반환.
     */
    public List<UserPerfStatResponse> getPerfStats(Long userId, OAuthPlatForm platform, String timeClass) {
        return perfStatRepository.findByUserIdAndPlatform(userId, platform).stream()
            .filter(s -> timeClass == null || timeClass.equals(s.getTimeClass()))
            .map(UserPerfStatResponse::from)
            .toList();
    }

    /**
     * 최근 1년간 월별 마지막 레이팅 변화.
     * timeClass 지정 시 해당 타입만, null이면 전체 타임클래스.
     */
    public RatingHistoryResponse getRatingHistory(Long userId, OAuthPlatForm platform, String timeClass) {
        YearMonth now = YearMonth.now();
        YearMonth oneYearAgo = now.minusMonths(11);

        List<MonthlyRatingEntry> data = monthlyRatingStatRepository
            .findByUserIdAndPlatform(userId, platform)
            .stream()
            .filter(s -> timeClass == null || timeClass.equals(s.getTimeClass()))
            .filter(s -> {
                YearMonth ym = YearMonth.of(s.getYear(), s.getMonth());
                return !ym.isBefore(oneYearAgo) && !ym.isAfter(now);
            })
            .map(s -> new MonthlyRatingEntry(
                YearMonth.of(s.getYear(), s.getMonth()).format(YEAR_MONTH_FORMAT),
                s.getTimeClass(),
                s.getRating()
            ))
            .toList();

        return new RatingHistoryResponse(
            oneYearAgo.format(YEAR_MONTH_FORMAT),
            now.format(YEAR_MONTH_FORMAT),
            data
        );
    }
}
