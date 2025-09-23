package capstone.chatbot.service;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatAskRequest {
    @NotBlank
    private String userId;

    @NotBlank
    private String question;
}

