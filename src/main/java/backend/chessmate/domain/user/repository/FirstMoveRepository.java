package backend.chessmate.domain.user.repository;

import backend.chessmate.domain.auth.entity.User;
import backend.chessmate.domain.user.entity.FirstMove;
import backend.chessmate.domain.user.entity.Streak;
import backend.chessmate.domain.user.entity.key.FirstMoveId;
import backend.chessmate.domain.user.entity.key.OpeningId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FirstMoveRepository extends JpaRepository<FirstMove, FirstMoveId> {

    @Query("""
        SELECT fm
        FROM FirstMove fm 
        WHERE fm.user = :user 
        ORDER BY fm.count DESC
        LIMIT :top 
        """)
    List<FirstMove> findTopFirstMovesByUser(
            @Param("user") User user,
            @Param("top") int top
    );

    @Modifying
    @Query(value = """
    INSERT INTO first_move (user_id, first_move, count)
    VALUES (:userId, :firstMove, 1)
    ON DUPLICATE KEY UPDATE count = count + 1
""", nativeQuery = true)
    void upsertFirstMove(@Param("userId") Long userId, @Param("firstMove") String firstMove);
}
