package backend.chessmate.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

@Getter
@Configuration
@ConfigurationProperties(prefix = "spring.data.redis.key")
public class RedisKeyProperties {

  private final Oauth oauth = new Oauth();
  private final User user = new User();
  private final Auth auth = new Auth();

  /* =========================
     OAuth 캐시 키
   ========================= */
  @Getter @Setter
  public static class Oauth {
    private String pkce;
    private String lichessToken;

    public String pkce(String state) {
      return pkce + ":" + state;
    }

    public String lichessToken(String lichessId) {
      return lichessToken + ":" + lichessId;
    }
  }

  /* =========================
     User 캐시 키
   ========================= */
  @Getter @Setter
  public static class User {
    private String playtime;
    private String perfs;
    private String playcount;

    public String playtime(String lichessId) {
      return playtime + ":" + lichessId;
    }

    public String perfs(String lichessId) {
      return perfs + ":" + lichessId;
    }

    public String playCount(String lichessId) {
      return playcount + ":" + lichessId;
    }
  }

  /* =========================
     Auth 캐시 키
   ========================= */
  @Getter @Setter
  public static class Auth {
    private String refreshToken;

    public String refreshToken(Long userId) {
      return refreshToken + ":" + userId;
    }
  }
}
