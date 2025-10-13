package backend.chessmate.domain.user.dto.response;

import backend.chessmate.domain.user.dto.api.UserPerf;
import backend.chessmate.domain.user.dto.response.tier.TierResult;
import backend.chessmate.domain.user.utils.TierUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserPerfResponse {

    private TierResult nowTier;
    private TierResult maxTier;
    private TierResult minTier;


    private Integer totalGames;
    private Long playTime;//초 기준
    private Double percentile; //상위 몇프로인지? 백분율

    private Integer winCount;
    private Integer lossCount;
    private Integer drawCount;
    private Double winRate;

    private Integer maxWinningStreak; //최대 연승
    private Integer maxLosingStreak; //최대 연패

    private Integer nowWinningStreak; //현재 연승
    private Integer nowLosingStreak; //현재 연패

    public static UserPerfResponse from(UserPerf p, TierUtil tierUtil) {
        var stat = p.getStat();

        // Optional로 null-safe하게 rating 가져오기
        Integer highestRating = Optional.ofNullable(stat)
                .map(s -> s.getHighest())
                .map(h -> h.getRating())
                .orElse(0);  // 기본값 0 or 필요에 따라 null 처리 가능

        Integer lowestRating = Optional.ofNullable(stat)
                .map(s -> s.getLowest())
                .map(l -> l.getRating())
                .orElse(0);

        var glicko = p.getPerf().getGlicko();
        Integer nowRating = (int) glicko.getRating();

        var count = stat.getCount();
        Integer totalGames = Optional.ofNullable(count)
                .map(c -> c.getAll())
                .orElse(0);

        Integer winCount = Optional.ofNullable(count)
                .map(c -> c.getWin())
                .orElse(0);

        Integer lossCount = Optional.ofNullable(count)
                .map(c -> c.getLoss())
                .orElse(0);

        Integer drawCount = Optional.ofNullable(count)
                .map(c -> c.getDraw())
                .orElse(0);

        Long playTime = Optional.ofNullable(count)
                .map(c -> c.getSeconds())
                .orElse(0L);

        var streak = stat.getResultStreak();

        Integer maxWinningStreak = Optional.ofNullable(streak)
                .map(st -> st.getWin())
                .map(win -> win.getMax())
                .map(max -> max.getValue())
                .orElse(0);

        Integer maxLosingStreak = Optional.ofNullable(streak)
                .map(st -> st.getLoss())
                .map(loss -> loss.getMax())
                .map(max -> max.getValue())
                .orElse(0);

        Integer nowWinningStreak = Optional.ofNullable(streak)
                .map(st -> st.getWin())
                .map(win -> win.getCur())
                .map(cur -> cur.getValue())
                .orElse(0);

        Integer nowLosingStreak = Optional.ofNullable(streak)
                .map(st -> st.getLoss())
                .map(loss -> loss.getCur())
                .map(cur -> cur.getValue())
                .orElse(0);

        Double percentile = Optional.ofNullable(p.getPercentile())
                .orElse(0.0);

        TierResult nowTierResult = tierUtil.calculateTier(nowRating);
        TierResult maxTierResult = tierUtil.calculateTier(highestRating);
        TierResult minTierResult = tierUtil.calculateTier(lowestRating);

        double winRate = 0.0;
        if (totalGames > 0 && winCount != null) {
            winRate = (double) winCount / totalGames * 100;
        }

        return UserPerfResponse.builder()
                .nowTier(nowTierResult)
                .maxTier(maxTierResult)
                .minTier(minTierResult)
                .totalGames(totalGames)
                .playTime(playTime)
                .percentile(percentile)
                .winCount(winCount)
                .lossCount(lossCount)
                .drawCount(drawCount)
                .winRate(winRate)
                .maxWinningStreak(maxWinningStreak)
                .maxLosingStreak(maxLosingStreak)
                .nowWinningStreak(nowWinningStreak)
                .nowLosingStreak(nowLosingStreak)
                .build();
    }

}


