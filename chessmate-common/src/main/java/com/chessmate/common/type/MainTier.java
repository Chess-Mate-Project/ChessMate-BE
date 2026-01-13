package com.chessmate.common.type;

public enum MainTier {
  PAWN("PAWN", 400, 900),
  KNIGHT("KNIGHT", 901, 1200),
  BISHOP("BISHOP", 1201, 1500),
  ROOK("ROOK", 1501, 1800),
  QUEEN("QUEEN", 1801, 2100),
  KING("KING", 2101, Integer.MAX_VALUE);

  private final String name;
  private final int minRating;
  private final int maxRating;

  MainTier(String name, int minRating, int maxRating) {
    this.name = name;
    this.minRating = minRating;
    this.maxRating = maxRating;
  }

  public String getName() {
    return name;
  }

  public int getMinRating() {
    return minRating;
  }

  public int getMaxRating() {
    return maxRating;
  }

  public static MainTier getTierByRating(int rating) {
    for (MainTier tier : MainTier.values()) {
      if (rating >= tier.minRating && rating <= tier.maxRating) {
        return tier;
      }
    }
    return PAWN;
  }
}
