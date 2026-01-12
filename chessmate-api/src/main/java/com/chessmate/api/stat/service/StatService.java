package com.chessmate.api.stat.service;

import com.chessmate.api.stat.dto.DailyStreakDto;
import com.chessmate.api.stat.dto.YearStreakDto;
import com.chessmate.domain.user.User;
import com.chessmate.infra_persistence.repositoryImpl.UserColorStatRepositoryImpl;
import com.chessmate.infra_persistence.repositoryImpl.UserDailyStreakRepositoryImpl;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StatService {

  private final UserDailyStreakRepositoryImpl userDailyStreakRepository;
  private final UserColorStatRepositoryImpl userColorStatRepository;
    
  @Transactional(readOnly = true)
  public YearStreakDto getDailyStreaksByYear(User user, Year year) {

    List<DailyStreakDto> dailyStreakDto = new ArrayList<>();

    LocalDate start = LocalDate.of(year.getValue(), 1, 1);
    LocalDate end = LocalDate.of(year.getValue(), 12, 31);

    userDailyStreakRepository.findByUserIdAndYearRange(1L, start, end)
        .forEach(streak -> {

              DailyStreakDto dto = new DailyStreakDto(
                  streak.getDate(),
                  streak.getWin(),
                  streak.getLose(),
                  streak.getDraw(),
                  (streak.getWin() + streak.getLose() + streak.getDraw())
              );

              dailyStreakDto.add(dto);

            }
        );

    return new YearStreakDto(year, dailyStreakDto);
  }

  @Transactional
  public void getColorStats(User user) {

  }
}
