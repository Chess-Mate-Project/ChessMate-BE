package com.chessmate.api.rank.dto;

import com.chessmate.common.dto.TierResult;

public record RankerDto (
    String profileImageUrl,
    String bannerImageUrl,
    String username,
    TierResult tierResult
) {
}
