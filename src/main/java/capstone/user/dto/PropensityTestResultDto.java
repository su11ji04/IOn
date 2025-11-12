package capstone.user.dto;

import lombok.NoArgsConstructor;
import lombok.*;
import java.util.*;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropensityTestResultDto {
    private List<Map<Integer, Integer>> listScores; //<id, sccore>
    private String userType;                     // authoritative / authoritarian / permissive 중 1
    private List<Map<String, Double>> testScores; // [{ "authoritative": 3.5, ... }] 형태(요구 스펙)
    private List<SubtypeScoreDto> subtype;       // 하위 타입 배열

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SubtypeScoreDto {
        private String type;   // "Warmth & Support" 등
        private Double score;  // 평균 점수
    }
}
