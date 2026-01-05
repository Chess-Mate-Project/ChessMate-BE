package backend.chessmate.api.external.dto.account;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 게임 타입별 퍼포먼스 묶음 DTO
 *
 * - 각 필드는 하나의 게임 모드에 해당
 * - 모든 필드는 동일한 PerfDto 구조를 사용
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PerfsDto(

    PerfDto bullet,          // 초단기전 (1분 이하)
    PerfDto blitz,           // 블리츠 (3~5분)
    PerfDto rapid,           // 래피드 (10~15분)
    PerfDto classical,       // 클래시컬
    PerfDto correspondence,  // 우편 체스
    PerfDto chess960,        // 체스960
    PerfDto kingOfTheHill,   // King of the Hill
    PerfDto threeCheck,      // Three-Check
    PerfDto antichess,       // 안티체스
    PerfDto atomic,          // 아토믹
    PerfDto horde,           // 호드
    PerfDto crazyhouse,      // 크레이지하우스
    PerfDto puzzle           // 퍼즐 레이팅
) {
}


