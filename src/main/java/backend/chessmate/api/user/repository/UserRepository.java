package backend.chessmate.api.user.repository;

import backend.chessmate.api.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByLichessId(String lichessId);
    Optional<User> findByLichessId(String lichessId);
    Optional<User> findByUsername(String username);
}
