package backend.chessmate.domain.user.entity.type;

public enum GameType {
    BLITZ,
    BULLET,
    RAPID,
    CLASSICAL,

    //api 호출 전용
    blitz,
    bullet,
    rapid,
    classical
}
