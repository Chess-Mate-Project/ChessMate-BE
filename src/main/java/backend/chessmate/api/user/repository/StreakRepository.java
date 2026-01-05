package backend.chessmate.api.user.repository;

import backend.chessmate.api.user.entity.User;
import backend.chessmate.api.user.entity.Streak;
import backend.chessmate.api.user.entity.key.StreakId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StreakRepository extends JpaRepository<Streak, StreakId> {
    @Query("""
        SELECT s 
        FROM Streak s 
        WHERE s.user = :user 
          AND FUNCTION('YEAR', s.date) = :year
        ORDER BY s.date ASC
        """)
    List<Streak> findAllByUserAndYear(
            @Param("user") User user,
            @Param("year") int year
    );

    @Query("""
        SELECT s.lastMoveAt
        FROM Streak s
        WHERE s.user = :user 
        ORDER BY s.lastMoveAt DESC
        limit 1
    """
    )
    Optional<Long> findLastMoveAtByUser(
            @Param("user") User user
    );

//    @Modifying
//    @Query(value = """
//INSERT INTO streak (user_id, date, total, win, lose, draw)
//VALUES (:userId, :date, :total, :win, :lose, :draw)X
//ON DUPLICATE KEY UPDATE
//total = :total, win = :win, lose = :lose, draw = :draw
//""", nativeQuery = true)
//    void upsertStreak(Long userId, LocalDate date, int total, int win, int lose, int draw);

}
