package com.chessmate.api.rank.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class MyRankInfo {
  private boolean loggedInUser;

  private int rank;
  private int rating;
  private Long userId;
  private String username;
  private String banner;
  private String profile;
  private String description;

  public static MyRankInfo notLoggedIn() {
    return new MyRankInfo(false, 0, 0, null, null, null, null, null);
  }
}
