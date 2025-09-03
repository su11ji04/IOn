package capstone.workbook.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ActivityItem {
    private String type;              // 작성형/선택형/시뮬레이션
    private String instruction;
    private String example_answer;    // 작성형 전용
    private List<String> options;     // 선택형 전용
    private String optimal_option;    // 선택형 전용
    private String situation;         // 시뮬레이션 전용
    private String your_response;     // 시뮬레이션 전용
    private String ai_optimal_response; // 시뮬레이션 전용
}
