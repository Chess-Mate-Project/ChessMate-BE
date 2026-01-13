package com.chessmate.common.dto;

import com.chessmate.common.type.MainTier;
import com.chessmate.common.type.SubTier;

public class TierResult {
  private MainTier mainTier;
  private SubTier subTier;
  private int rating;

  public TierResult(int rating) {
    this.rating = rating;
    MainTier tier = MainTier.getTierByRating(rating);
    this.mainTier = tier;
    this.subTier = calculateSubTier(tier, rating);
  }

  private SubTier calculateSubTier(MainTier tier, int rating) {
    int tierMin = tier.getMinRating();
    int relativeRating = rating - tierMin;
    int levelIndex = relativeRating / 60;

    if (levelIndex >= 5) {
      return SubTier.I;
    }
    return SubTier.values()[4 - levelIndex];
  }

  public MainTier getMainTier() {
    return mainTier;
  }

  public SubTier getSubTier() {
    return subTier;
  }

  public int getRating() {
    return rating;
  }

  @Override
  public String toString() {
    return mainTier.getName() + " " + subTier.name();
  }
}
