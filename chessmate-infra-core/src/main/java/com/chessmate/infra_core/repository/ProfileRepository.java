package com.chessmate.infra_core.repository;

import com.chessmate.infra_core.entity.Profile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository <Profile, Long>{
  Optional<Profile> findById(Long id);
}
