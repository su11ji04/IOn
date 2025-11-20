package capstone.chatbot.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotListDto {
    private int sessionId;
    private List<QuestionAnswerDto> questions;
}
