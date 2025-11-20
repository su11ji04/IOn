package capstone.chatbot.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionAnswerDto {
    private String question;
    private String answer;
}
