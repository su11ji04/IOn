package capstone.chatbot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ChatQuestion {
    private String chatQuestionId;
    @NotBlank
    private String userId;
    @NotBlank
    private String question;
}
