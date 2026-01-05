package backend.chessmate.api.user.repository;

import backend.chessmate.api.user.entity.User;
import backend.chessmate.api.user.entity.Opening;
import backend.chessmate.api.user.entity.key.OpeningId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OpeningRepository extends JpaRepository<Opening, OpeningId> {

    @Query("""
        SELECT o
        FROM Opening  o
        WHERE o.user = :user 
        ORDER BY o.count DESC
        LIMIT :top
        """)
    List<Opening> findTopOpeningsByUser(
            @Param("user") User user,
            @Param("top") int top
    );
//    @Modifying
//    @Query(value = """
//    INSERT INTO opening (user_id, opening, count)
//    VALUES (:userId, :opening, 1)
//    ON DUPLICATE KEY UPDATE count = count + 1
//""", nativeQuery = true)
//    void upsertOpening(@Param("userId") Long userId, @Param("opening") String opening);
}
