package com.chessmate.external.dto.chesscom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CertInfo {
  public String kid;
  public String kty;
  public String alg;
  public String n;
  public String e;
}