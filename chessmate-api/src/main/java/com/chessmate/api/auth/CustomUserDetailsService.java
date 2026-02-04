package com.chessmate.api.auth;


import com.chessmate.common.code.UserErrorCode;
import com.chessmate.common.exception.UserException;
import com.chessmate.domain.user.User;
import com.chessmate.infra_persistence.entity.UserEntity;
import com.chessmate.infra_persistence.repositoryImpl.UserRepositoryImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepositoryImpl userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        log.info(username);
      User user = userRepository.findById(Long.valueOf(username))
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND_USER));

        return new UserPrincipal(user);
    }
}

