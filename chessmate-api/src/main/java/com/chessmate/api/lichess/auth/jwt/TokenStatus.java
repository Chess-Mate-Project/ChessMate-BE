package com.chessmate.api.lichess.auth.jwt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum TokenStatus {
    AUTHENTICATED, // 서명·유효기간 모두 정상
    EXPIRED,       // 유효기간 지남
    INVALID        // 서명 불일치 등 구조 이상
}