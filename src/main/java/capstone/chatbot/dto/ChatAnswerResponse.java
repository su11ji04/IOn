package capstone.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.Map;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatAnswerResponse {
    private String answer; // 파이썬이 생성한 최종 답변 텍스트
    private String usedUserId; // 서버에 넘긴 userId 에코백 (디버그용)
    private Map<String, Object> meta; // 점수/참고문단 등 부가정보(선택)
}
