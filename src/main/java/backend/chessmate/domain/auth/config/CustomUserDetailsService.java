package backend.chessmate.domain.auth.config;

import backend.chessmate.domain.auth.entity.User;
import backend.chessmate.domain.auth.repository.UserRepository;
import backend.chessmate.global.common.code.UserErrorCode;
import backend.chessmate.global.common.exception.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetailsService;


@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findById(username)
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND_USER));

        return new UserPrincipal(user);
    }
}

