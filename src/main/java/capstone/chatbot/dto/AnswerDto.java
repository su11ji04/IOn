package capstone.chatbot.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnswerDto {
    private Integer sessionId;
    private String answer;
}
