package com.chessmate.api.oauth;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

public class OAuth2PrincipalDetails implements OAuth2User {
  private final Long id; // 우리 DB의 Profile PK
  private final Map<String, Object> attributes;

  public OAuth2PrincipalDetails(Long id, Map<String, Object> attributes) {
    this.id = id;
    this.attributes = attributes;
  }

  @Override public Map<String, Object> getAttributes() { return attributes; }
  @Override public Collection<? extends GrantedAuthority> getAuthorities() { return Collections.emptyList(); }
  @Override public String getName() { return String.valueOf(id); } // 중요: 식별자 반환
  public Long getId() { return id; }
}
