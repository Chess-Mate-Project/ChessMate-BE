package com.chessmate.domain.user;


import java.util.Optional;

public interface UserRepository {
    boolean existsByLichessId(String lichessId);
    Optional<User> findByLichessId(String lichessId);
    Optional<User> findByUsername(String username);
    Optional<User> findById(Long id);
    User save(User user);
    int count();

    void updateProfileImage(Long userId, String profileImageUrl);
    void updateBannerImage(Long userId, String bannerImageUrl);

}
