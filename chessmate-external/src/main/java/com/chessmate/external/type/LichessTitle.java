package com.chessmate.external.type;

/*
* Lichess에서 사용하는 체스 타이틀을 나타내는 열거형입니다.
* 각 타이틀은 약어와 전체 이름을 포함합니다.
* 예를 들어, GM은 "Grandmaster"를 나타냅니다.
* */
public enum LichessTitle {
  GM("Grandmaster"),
  IM("International Master"),
  FM("FIDE Master"),
  CM("Candidate Master"),
  WGM("Woman Grandmaster"),
  WIM("Woman International Master"),
  WFM("Woman FIDE Master"),
  WCM("Woman Candidate Master"),
  LM("Lichess Master");

  private final String title;

  LichessTitle(String title) {
    this.title = title;
  }

  public String getTitle() {
    return title;
  }

  @Override
  public String toString() {
    return title;
  }
}
