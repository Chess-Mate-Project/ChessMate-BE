package backend.chessmate.domain.user.service;


import backend.chessmate.domain.auth.entity.User;
import backend.chessmate.domain.user.dto.FirstMoveDto;
import backend.chessmate.domain.user.dto.OpeningDto;
import backend.chessmate.domain.user.dto.StreakDto;
import backend.chessmate.domain.user.dto.UserPlayCountDto;
import backend.chessmate.domain.user.dto.api.UserAccount;
import backend.chessmate.domain.user.dto.response.streak.UserStreak;
import backend.chessmate.domain.user.dto.response.streak.UserStreaksResponse;
import backend.chessmate.domain.user.entity.FirstMove;
import backend.chessmate.domain.user.entity.Opening;
import backend.chessmate.domain.user.entity.Streak;
import backend.chessmate.domain.user.repository.FirstMoveRepository;
import backend.chessmate.domain.user.repository.OpeningRepository;
import backend.chessmate.domain.user.repository.StreakRepository;
import backend.chessmate.domain.user.utils.LichessUtil;
import backend.chessmate.global.config.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final StreakRepository streakRepository;
    private final OpeningRepository openingRepository;
    private final FirstMoveRepository firstMoveRepository;
    private final RedisService redisService;
    private final LichessUtil lichessUtil;


    @Value("${spring.data.redis.key.oauth_key_base}")
    private String OAUTH_KEY;

    @Value("${spring.data.redis.key.play_count_base}")
    private String PLAY_COUNT_KEY;

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

    public UserPlayCountDto getPlayCount(User u) {
        String oauthKey = OAUTH_KEY + ":" + u.getId();
        String oauthToken = redisService.get(oauthKey, String.class); // 유저 고유 lichess oauth api key

        UserAccount userAccount = lichessUtil.getUserAccount(oauthToken); // lichess api (account) 조회

        var key = PLAY_COUNT_KEY + ":"; // 레디스 저장 및 조회용 playCount Key

        if (redisService.get(key, UserPlayCountDto.class) != null) { // 레디스에 playCount가 존재하면
            return redisService.get(key, UserPlayCountDto.class); // 바로 꺼내서 반환
        }
        UserPlayCountDto userPlayCountDto = new UserPlayCountDto( //존재하지 않으면 새로운 객체 생성
                userAccount.getCount().getAll(),
                userAccount.getCount().getWin(),
                userAccount.getCount().getDraw(),
                userAccount.getCount().getLoss()
        );
        redisService.save(key, userPlayCountDto, 3600); // 1시간 레디스 저장 후
        return userPlayCountDto; // 반환

    }

}
