package com.chessmate.api.lichess.oauth;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

    OAuth2User oauth2User = super.loadUser(userRequest);

    String provider = userRequest.getClientRegistration().getRegistrationId();
    String providerId  = "";
    String username = oauth2User.getAttribute("username");


    if (provider.equals("chesscom")) {
      providerId = oauth2User.getAttribute("player_id");
      Chess

    } else if (provider.equals("lichess")) {
      providerId = oauth2User.getAttribute("id");
    }

  }
}
