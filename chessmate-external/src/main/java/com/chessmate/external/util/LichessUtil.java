package com.chessmate.external.util;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Random;

public class LichessUtil {

  public static String generateRandomCodeVerifier() {
    byte[] bytes = new byte[32];
    new Random().nextBytes(bytes);
    String code_verifier = encodeToString(bytes);
    return code_verifier;
  }


  static String encodeToString(byte[] bytes) {
    return Base64.getUrlEncoder().encodeToString(bytes)
        .replaceAll(  "=",  "")
        .replaceAll("\\+", "-")
        .replaceAll("\\/", "_");
  }

  public static String generateCodeChallenge(String code_verifier) {
    byte[] asciiBytes = code_verifier.getBytes(StandardCharsets.US_ASCII);
    MessageDigest md;

    try {
      md = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException nsa_ehhh) {
      throw new RuntimeException(nsa_ehhh);
    }

    byte[] s256bytes = md.digest(asciiBytes);

    String code_challenge = encodeToString(s256bytes);
    return code_challenge;
  }

  public static String generateRandomState() {
    byte[] bytes = new byte[16];
    new Random().nextBytes(bytes);
    // Not sure how long the parameter "should" be,
    // going for 8 characters here...
    return encodeToString(bytes).substring(0,8);
  }
}
