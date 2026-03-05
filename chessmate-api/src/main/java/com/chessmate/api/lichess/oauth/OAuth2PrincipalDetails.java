package com.chessmate.api.lichess.oauth;

import com.chessmate.domain.user.User;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

public class OAuth2PrincipalDetails implements OAuth2User {
  private User user;
  private Map<String , Object> attributes;

  public OAuth2PrincipalDetails(User user, Map<String, Object> attributes) {
    this.user = user;
    this.attributes = attributes;
  }

  @Override public Map<String, Object> getAttributes() { return attributes; }
  @Override public Collection<? extends GrantedAuthority> getAuthorities() { return Collections.emptyList(); }
  @Override public String getName() { return user.getUsername(); }
  public User getUser() { return user; }
}
