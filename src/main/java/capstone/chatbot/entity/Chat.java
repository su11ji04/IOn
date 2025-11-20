package capstone.chatbot.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "chatbot",
        indexes = {
                @Index(name = "idx_chatbot_user", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Integer sessionId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Builder.Default
    @ElementCollection
    @CollectionTable(
            name = "chating_group",
            joinColumns = @JoinColumn(name = "session_id")
    )
    @OrderColumn(name = "seq")
    private List<QuestionAndAnswer> questionAndAnswers = new ArrayList<>();

    @Column(name = "closed", nullable = false)
    @Builder.Default
    private boolean closed = false;
}