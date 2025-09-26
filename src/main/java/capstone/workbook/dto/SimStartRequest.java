package capstone.workbook.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SimStartRequest {
    private Long workbookId;
    private Integer stepIndex; // SIMULATION 활동 인덱스
    private String userId;
}
