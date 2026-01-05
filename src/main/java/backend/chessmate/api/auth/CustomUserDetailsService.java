package backend.chessmate.api.auth;

import backend.chessmate.api.user.entity.User;
import backend.chessmate.api.oauth.repository.UserRepository;
import backend.chessmate.global.common.code.UserErrorCode;
import backend.chessmate.global.common.exception.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetailsService;


@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        log.info(username);
        User user = userRepository.findById(Long.valueOf(username))
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND_USER));

        return new UserPrincipal(user);
    }
}

