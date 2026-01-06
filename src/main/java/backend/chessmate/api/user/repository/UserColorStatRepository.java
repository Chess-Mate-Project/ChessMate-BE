package backend.chessmate.api.user.repository;


import backend.chessmate.api.user.entity.User;
import backend.chessmate.api.user.entity.UserColorStat;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserColorStatRepository extends JpaRepository<UserColorStat, Long> {
}
