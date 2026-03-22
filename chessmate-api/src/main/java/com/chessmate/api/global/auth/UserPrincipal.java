package com.chessmate.api.global.auth;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class UserPrincipal implements UserDetails {
    private final Long id;
    private OAuth2Provider provider;

    public UserPrincipal(Long id, OAuth2Provider provider) {
      this.id = id;
      this.provider = provider;
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
