package capstone.chatbot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Lob;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionAndAnswer {
    @Lob
    @Column(name = "question", columnDefinition = "TEXT")
    private String question;

    @Lob
    @Column(name = "answer", columnDefinition = "TEXT")
    private String answer;
}
