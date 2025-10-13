package backend.chessmate.domain.user.service;


import backend.chessmate.domain.auth.entity.User;
import backend.chessmate.domain.user.dto.FirstMoveDto;
import backend.chessmate.domain.user.dto.OpeningDto;
import backend.chessmate.domain.user.dto.StreakDto;
import backend.chessmate.domain.user.dto.response.streak.UserStreak;
import backend.chessmate.domain.user.dto.response.streak.UserStreaksResponse;
import backend.chessmate.domain.user.entity.FirstMove;
import backend.chessmate.domain.user.entity.Opening;
import backend.chessmate.domain.user.entity.Streak;
import backend.chessmate.domain.user.repository.FirstMoveRepository;
import backend.chessmate.domain.user.repository.OpeningRepository;
import backend.chessmate.domain.user.repository.StreakRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final StreakRepository streakRepository;
    private final OpeningRepository openingRepository;
    private final FirstMoveRepository firstMoveRepository;

    public List<StreakDto> getStreak(User u, int year) {
        List<Streak> streaks = streakRepository.findAllByUserAndYear(u, year);

        return streaks.stream().map(s ->
                new StreakDto(s.getDate(),
                        s.getWin(),
                        s.getLose(),
                        s.getDraw()
                )).toList();
    }

    public List<FirstMoveDto> getFirstMove(User u, int top) {
        List<FirstMove> firstMoves = firstMoveRepository.findTopFirstMovesByUser(u, top);

        return firstMoves.stream().map(fm ->
                new FirstMoveDto(
                        fm.getMove(),
                        fm.getCount()
                )).toList();
    }

    public List<OpeningDto> getOpening(User u, int top) {
        List<Opening> openings = openingRepository.findTopOpeningsByUser(u, top);

        return openings.stream().map(o ->
                new OpeningDto(
                        o.getOpening(),
                        o.getCount()
                )).toList();
    }

}
