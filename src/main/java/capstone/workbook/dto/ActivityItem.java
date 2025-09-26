package capstone.workbook.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ActivityItem {
    private ActivityType type;

    // 시뮬레이션 상활 설정
    private String instruction;

    // [STEP 1] 선택형
    private List<String> options;
    private String optimal_option;

    // [STEP 2] 선택형
    private String example_answer;

    // [STEP 3] 시뮬레이션
    private String situation;
    private String ai_optimal_response;
}
