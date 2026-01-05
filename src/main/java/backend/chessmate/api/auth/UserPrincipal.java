package backend.chessmate.api.auth;

import backend.chessmate.api.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class UserPrincipal implements UserDetails {
    private final User user;
    private final Long id;

    public UserPrincipal(User u) {
        this.user = u;
        this.id = u.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(); // 권한이 필요하다면 여기에 추가
    }

    @Override
    public String getPassword() {
        return null; // JWT 인증 방식이라면 패스워드 필요 없음
    }

    @Override
    public String getUsername() {
        return String.valueOf(id);
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }

}
