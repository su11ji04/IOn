package capstone.chatbot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatAskRequest {
    /** UI에서 입력한 질문 */
    @NotBlank
    private String question;
}
