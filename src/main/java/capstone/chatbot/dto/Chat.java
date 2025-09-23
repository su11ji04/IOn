package capstone.chatbot.dto;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "chat")
public class Chat {

    @Id
    @Column(length = 36)
    private String id;

    @PrePersist
    void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
        if (this.chatQuestionId == null) this.chatQuestionId = UUID.randomUUID().toString();
        if (this.chatAnswerId == null) this.chatAnswerId = UUID.randomUUID().toString();
    }

    @Column(nullable = false, length = 64)
    private String userId;

    @Column(nullable = false, length = 2000)
    private String question;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String answer;

    @Column(nullable = false, length = 36)
    private String chatQuestionId;

    @Column(nullable = false, length = 36)
    private String chatAnswerId;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
