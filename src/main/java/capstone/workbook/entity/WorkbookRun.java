package capstone.workbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(
        name = "workbook_run",
        indexes = {
                @Index(name = "idx_workbook_run_user", columnList = "user_id"),
                @Index(name = "idx_workbook_run_workbook_id", columnList = "workbook_id"),
                @Index(name = "idx_workbook_run_created_at", columnList = "created_at")
        }
)
public class WorkbookRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workbook_id", nullable = false)
    private Long workbookId;

    @Column(name = "user_id", length = 100, nullable = false)
    private String userId;

    // 진행 단계: MCQ → WRITING → SIM → DONE
    @Column(name = "step", length = 16, nullable = false)
    private String step;

    // 답변
    @Column(name = "mcq_answer", length = 200)
    private String mcqAnswer;
    @Lob
    @Column(name = "writing_text")
    private String writingText;
    @Lob
    @Column(name = "sim_answer")
    private String simAnswer;

    // 완료 여부
    @Column(name = "finished", nullable = false)
    private Boolean finished;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (finished == null) finished = false;
        if (step == null) step = "MCQ";
    }
}
