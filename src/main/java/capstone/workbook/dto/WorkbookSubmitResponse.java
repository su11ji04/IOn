package capstone.workbook.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkbookSubmitResponse {
    private Integer stepIndex;
    private Integer nextStepIndex;
    private Boolean isLastStep;

    private ActivityType handledType; // MCQ or WRITING
    private Boolean correct;          // MCQ일 때만
    private String  feedback;

    private Boolean ok;

    // ✅ 추가: 프런트가 다음 화면을 정확히 그릴 수 있게
    private ActivityType nextExpectedType;  // MCQ → WRITING → SIMULATION 순서 안내
    private Boolean shouldStartSimulation;  // WRITING 끝나면 true (프런트는 /sim/start 호출)
}