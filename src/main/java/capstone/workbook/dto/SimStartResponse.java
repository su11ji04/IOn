package capstone.workbook.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SimStartResponse {
    private String sessionId; // 서버 생성
    private String situation; // 최초 상황
    private String aiLine;    // AI(아이 역할)의 첫 대사
}
