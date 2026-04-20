package com.chessmate.api.global.auth.oauth.chesscom;

import com.chessmate.external.dto.chesscom.CertInfo;
import com.chessmate.external.dto.chesscom.ChesscomJwtCertResponse;
import com.chessmate.external.dto.chesscom.ChesscomUserInfo;
import com.chessmate.external.oauth.chesscom.ChesscomOauthApi;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChesscomUtil {

  private final ChesscomOauthApi chesscomOauthApi;

  /**
   * ID 토큰을 검증하고 페이로드의 모든 정보를 객체로 반환합니다.
   */
  public ChesscomUserInfo parseIdToken(String idToken) {
    try {
      // 1. 헤더에서 kid 추출 (어떤 열쇠인지 확인)
      String kid = getKidFromToken(idToken);

      // 2. Chess.com에서 공개키 목록 가져오기
      ChesscomJwtCertResponse response = chesscomOauthApi.getCert();

      // 3. 일치하는 키 찾기
      CertInfo matchingKey = response.keys.stream()
          .filter(key -> key.kid.equals(kid))
          .findFirst()
          .orElseThrow(() -> new RuntimeException("일치하는 공개키를 찾을 수 없습니다."));

      // 4. n, e 값을 이용해 PublicKey 객체로 변환 (수학적 열쇠 깎기)
      PublicKey publicKey = getPublicKey(matchingKey.n, matchingKey.e);

      // 5. 검증 및 데이터 추출
      Claims claims = Jwts.parserBuilder()
          .setSigningKey(publicKey) // 검증용 열쇠 세팅
          .build()
          .parseClaimsJws(idToken)   // 서명/만료시간 검증 및 변환
          .getBody();

      // 6. 결과 담기 (기존 sub 포함 모든 데이터)
      ChesscomUserInfo userInfo = new ChesscomUserInfo();
      userInfo.setSub(claims.getSubject()); // 여기서 기존 sub를 가져옵니다!
      userInfo.setEmail(claims.get("email", String.class));
      userInfo.setUsername(claims.get("preferred_username", String.class));
      userInfo.setUserId(claims.get("user_id", String.class));
      userInfo.setPicture(claims.get("picture", String.class));
      userInfo.setCountry(claims.get("country", String.class));
      userInfo.setMembership(claims.get("membership", String.class));

      return userInfo;

    } catch (Exception ex) {
      System.err.println("Chess.com 토큰 검증 실패: " + ex.getMessage());
      return null;
    }
  }

  private String getKidFromToken(String idToken) {
    int i = idToken.lastIndexOf('.');
    String withoutSignature = idToken.substring(0, i + 1);
    Jwt<Header, Claims> untrusted = Jwts.parserBuilder().build().parseClaimsJwt(withoutSignature);
    return (String) untrusted.getHeader().get("kid");
  }

  public PublicKey getPublicKey(String nStr, String eStr) throws Exception {
    byte[] nBytes = Base64.getUrlDecoder().decode(nStr);
    byte[] eBytes = Base64.getUrlDecoder().decode(eStr);
    BigInteger n = new BigInteger(1, nBytes);
    BigInteger e = new BigInteger(1, eBytes);
    RSAPublicKeySpec spec = new RSAPublicKeySpec(n, e);
    return KeyFactory.getInstance("RSA").generatePublic(spec);
  }
}