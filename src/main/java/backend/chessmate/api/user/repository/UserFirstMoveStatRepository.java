package backend.chessmate.api.user.repository;

import backend.chessmate.api.user.entity.UserColorStat;
import backend.chessmate.api.user.entity.UserFirstMoveStat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFirstMoveStatRepository extends JpaRepository<UserFirstMoveStat, Long> {
}