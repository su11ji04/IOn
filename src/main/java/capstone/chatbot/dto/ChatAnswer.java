package capstone.chatbot.dto;
import lombok.*;
import java.util.Map;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ChatAnswer {
    private String chatAnswerId;
    private String chatQuestionId;
    private String chatId;
    private String answer;
}
