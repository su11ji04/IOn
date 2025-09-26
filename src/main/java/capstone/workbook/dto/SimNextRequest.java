package capstone.workbook.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SimNextRequest {
    private String sessionId;
    private String userReply; // 사용자의 답변(문자열)
}