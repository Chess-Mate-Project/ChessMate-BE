package com.chessmate.api.lichess.rank.dto;

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
  private boolean loggedInUser; // 로그인유저임? true or false
  private boolean unrated; // 언레이팅임? true or false

  private int rank;
  private int rating;
  private Long userId;
  private String username;
  private String banner;
  private String profile;
  private String description;

  public static MyRankInfo imNotLoginUser() {
    return new MyRankInfo(false, false, 0, 0, null, null, null, null, null);
  }
}
