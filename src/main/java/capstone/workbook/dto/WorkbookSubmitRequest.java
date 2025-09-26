package capstone.workbook.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkbookSubmitRequest {
    private Long workbookId;     // 진행 중인 워크북 레코드 id
    private Integer stepIndex;   // activities의 인덱스 (0..n-1)
    private String userId;       // u001 고정 등
    private String userAnswer;   // 선택형/작성형 공통으로 문자열
}
