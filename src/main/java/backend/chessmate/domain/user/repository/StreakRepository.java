package backend.chessmate.domain.user.repository;

import backend.chessmate.domain.auth.entity.User;
import backend.chessmate.domain.user.entity.Streak;
import backend.chessmate.domain.user.entity.key.StreakId;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

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

    @Modifying
    @Query(value = """
INSERT INTO streak (user_id, date, total, win, lose, draw)
VALUES (:userId, :date, :total, :win, :lose, :draw)X
ON DUPLICATE KEY UPDATE
total = :total, win = :win, lose = :lose, draw = :draw
""", nativeQuery = true)
    void upsertStreak(Long userId, LocalDate date, int total, int win, int lose, int draw);

}
