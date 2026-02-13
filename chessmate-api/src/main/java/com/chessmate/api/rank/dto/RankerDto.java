package com.chessmate.api.rank.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RankerDto {

  private Long userId;

  private String username;

  private String title;

  private int rating;

  private int rank;

  private String bannerImage;

  private String profileImage;

}

