//package com.chessmate.api.rank.controller;
//
//import com.chessmate.api.rank.service.RankService;
//import com.chessmate.common.response.SuccessResponse;
//import com.chessmate.common.type.GameType;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.RequestEntity;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequiredArgsConstructor
//public class RankController {
//
//  private final RankService rankService;
//
//
//  //인증 없이도 접근 가능
//  public RequestEntity<SuccessResponse<Void>> getRanks(
//      @RequestParam GameType gameType,
//      @RequestParam int page
//  ) {
//
//    rankService.getRankers(gameType, page);
//    return null;
//  }
//
//
//}
