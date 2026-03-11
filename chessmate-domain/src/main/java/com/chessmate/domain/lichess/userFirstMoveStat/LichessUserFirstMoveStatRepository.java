//package com.chessmate.domain.lichess.userFirstMoveStat;
//
//import com.chessmate.common.type.ChessColor;
//import com.chessmate.common.type.GameType;
//import java.util.List;
//
//public interface LichessUserFirstMoveStatRepository {
//  void saveAll(Iterable<LichessUserFirstMoveStat> entities);
//  List<LichessUserFirstMoveStat> findByUserIdAndGameType(Long userId, GameType gameType);
//  List<LichessUserFirstMoveStat> findByUserIdAndGameTypeAndColor(Long userId, GameType gameType, ChessColor color);
//
//  void deleteAllByUserId(Long userId);
//}
//
