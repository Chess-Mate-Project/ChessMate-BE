package com.chessmate.api.user.service;


import com.chessmate.api.user.dto.ProfileResponse;
import com.chessmate.api.user.dto.TotalUserCountResponse;
import com.chessmate.api.user.dto.UpdateUserDescriptionRequest;
import com.chessmate.common.code.UserErrorCode;
import com.chessmate.common.exception.AuthException;
import com.chessmate.common.exception.UserException;
import com.chessmate.domain.user.User;
import com.chessmate.infra_persistence.entity.UserEntity;
import com.chessmate.infra_persistence.repositoryImpl.UserRepositoryImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepositoryImpl userRepository;

  public TotalUserCountResponse getTotalUserCount() {
    long totalUsers = userRepository.count();
    return new TotalUserCountResponse((int) totalUsers);
  }

  /**
   * - 사용자 자기소개 업데이트 - @param user 현재 사용자 - @param updateUserDescriptionRequest 사용자 자기소개 업데이트 요청 DTO
   * - @return void
   *
   */

  public void updateUserDescription(User user,
      UpdateUserDescriptionRequest updateUserDescriptionRequest) {
    userRepository.findById(user.getId()).ifPresent(u -> {
      u.setDescription(updateUserDescriptionRequest.description());
      userRepository.save(u);
    });
  }

  public ProfileResponse getUserProfile(User user) {
    User u = userRepository.findById(user.getId()).orElseThrow(
        () -> new UserException(UserErrorCode.NOT_FOUND_USER)
    );

    return new ProfileResponse(
        u.getUsername(),
        u.getDescription(),
        u.getCreatedAt(),
        u.getLichessCreatedAt(),
        "https://lichess.org/@/" + u.getLichessId()
    );
  }

}
