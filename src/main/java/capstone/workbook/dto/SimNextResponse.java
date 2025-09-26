package capstone.workbook.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SimNextResponse {
    private String aiLine;        // 다음 턴의 아이 역할 대사
    private boolean finished;     // 종료 여부
    private String finalFeedback; // 종료 시 전체 피드백(없으면 null)
}