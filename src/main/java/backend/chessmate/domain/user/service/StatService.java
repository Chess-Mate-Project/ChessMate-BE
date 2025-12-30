package backend.chessmate.domain.user.service;

import backend.chessmate.domain.auth.entity.User;
import backend.chessmate.domain.auth.repository.UserRepository;
import backend.chessmate.domain.user.utils.UserStatsDto;
import backend.chessmate.global.common.code.UserErrorCode;
import backend.chessmate.global.common.exception.UserException;
import backend.chessmate.global.config.redis.RedisService;
import backend.chessmate.domain.user.repository.StreakRepository;
import backend.chessmate.domain.user.utils.LichessUtil;
import backend.chessmate.domain.user.utils.Status;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatService {
    private final StreakRepository streakRepository;
    private final EntityManager entityManager;
    private final LichessUtil lichessUtil;


    @Async("InitStreaks")
    @Transactional
    public void asyncGames(User u) {
        try {
            Long lastMoveAt = streakRepository.findLastMoveAtByUser(u).orElse(null);
            if (lastMoveAt == null) {
                initGames(u);
            } else {
                upsertGames(u, lastMoveAt);
            }
        } catch(Exception e) {

        }
    }

    /**
     * writer 단계
     * lichessUtils 에서 받아온 UserStatDto 정보를 전부 upsert
     */
    @Transactional
    public void initGames(User u) {
        UserStatsDto userStatsDto = lichessUtil.getUserGamesApi(u, 0L, System.currentTimeMillis());
        bulkUpsertAll(u, userStatsDto);
    }

    /**
     * writer 단계
     * lichessUtils 에서 받아온 UserStatDto 정보를 전부 upsert
     *
     */
    @Transactional
    public void upsertGames(User u, Long lastMoveAt) {
        UserStatsDto userStatsDto = lichessUtil.getUserGamesApi(u, lastMoveAt + 1,  System.currentTimeMillis());
        bulkUpsertAll(u, userStatsDto);





    }

    private void bulkUpsertAll(User u, UserStatsDto userStatsDto) {
        var statusByDate = userStatsDto.getStatusByDate();
        var countByOpening = userStatsDto.getCountByOpening();
        var countByFirstMove = userStatsDto.getCountByFirstMove();

        bulkUpsertStreaks(u, statusByDate);
        bulkUpsertOpenings(u, countByOpening);
        bulkUpsertFirstMove(u, countByFirstMove);
    }

    public void bulkUpsertStreaks(User u, Map<LocalDate, Status> streaks) {
        StringBuilder sql = new StringBuilder("INSERT INTO streak (user_id, date, win, lose, draw, last_move_at) VALUES ");
        List<String> valueTuples = new ArrayList<>();

        streaks.forEach((date, status) -> {
            String tuple = String.format(
                    "(%d, '%s', %d, %d, %d, %d)",
                    u.getId(),
                    date, // LocalDate -> 'YYYY-MM-DD'
                    status.getWin(),
                    status.getLose(),
                    status.getDraw(),
                    status.getLastMoveAt()
            );
            valueTuples.add(tuple);
        });

        sql.append(String.join(",", valueTuples));
        sql.append(" ON DUPLICATE KEY UPDATE ")
                .append("win = win + VALUES(win), ")
                .append("lose = lose + VALUES(lose), ")
                .append("draw = draw + VALUES(draw), ")
                .append("last_move_at = GREATEST(last_move_at, VALUES(last_move_at))");
        log.info("statusByDate bulk upsert {} ", sql.toString());
        entityManager.createNativeQuery(sql.toString()).executeUpdate();
    }


    public void bulkUpsertOpenings(User u, Map<String, Long> openings) {
        if (openings.isEmpty()) return;

        StringBuilder sql = new StringBuilder(
                "INSERT INTO opening (user_id, opening, count) VALUES "
        );

        List<String> valueTuples = new ArrayList<>();

        openings.forEach((opening, count) -> {

            String tuple = String.format(
                    "(%d, '%s', %d)",
                    u.getId(),
                    opening.replace("'", "''"),
                    count
            );
            valueTuples.add(tuple);
        });

        sql.append(String.join(",", valueTuples));
        sql.append(" ON DUPLICATE KEY UPDATE count = count + VALUES(count)");

        entityManager.createNativeQuery(sql.toString()).executeUpdate();
    }



    public void bulkUpsertFirstMove(User u, Map<String, Long> firstMoves) {
        if (firstMoves.isEmpty()) return;

        StringBuilder sql = new StringBuilder(
                "INSERT INTO first_move (user_id, move, count) VALUES "
        );

        List<String> valueTuples = new ArrayList<>();

        firstMoves.forEach((move, count) -> {
            String tuple = String.format(
                    "(%d, '%s', %d)",
                    u.getId(),
                    move,
                    count
            );
            valueTuples.add(tuple);
        });

        sql.append(String.join(",", valueTuples));
        sql.append(" ON DUPLICATE KEY UPDATE count = count + VALUES(count)");

        entityManager.createNativeQuery(sql.toString()).executeUpdate();
    }


//    // 사용자의 현재 스트릭을 update하는
//    @Async("defaultTask")
//    @Transactional
//    public void updateStreaks(User u) {
//        long since = u.getLastSyncedAt();
//        long until = System.currentTimeMillis();
//
//        Map<LocalDate, Status> statusByDate = lichessUtil.updateUserGameStreaks(u, since, until);
//        List<Streak>  entities = Streak.from(u, statusByDate);
//
//        for (Streak streak : entities) {
//            streaksRepository.upsertStreak(u.getId(), streak.getDate(), streak.getTotal(), streak.getWin(), streak.getLose(), streak.getDraw());
//        }
//        u.setLastSyncedAt(until);
//        userRepository.save(u);
//    }

}
