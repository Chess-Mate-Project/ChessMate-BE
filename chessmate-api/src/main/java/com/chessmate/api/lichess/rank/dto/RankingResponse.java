package com.chessmate.api.lichess.rank.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingResponse {

  private MyRankInfo myRankInfo;

  private List<RankerDto> ranking;

  private long totalCount;

  private int currentPage;

  private int pageSize;

  private long totalPages;
}


