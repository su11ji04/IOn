package capstone.workbook.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "workbook_run")
public class WorkbookRun {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private Long workbookId;
    @Column(nullable = false, length = 64) private String userId;

    // 진행 단계: MCQ → WRITING → SIM1 → SIM2 → FEEDBACK
    @Column(nullable = false, length = 16) private String step;

    // 답변 히스토리
    @Column(length = 200) private String mcqAnswer;     // 사용자 선택 텍스트
    @Lob private String writingText;                    // 서술형 답
    @Lob private String simHistoryJson;                 // [{"role":"ai|user","text":"..."}...]

    @Lob private String finalFeedbackJson;              // {"overall_comment":..., "tips":[...]}
    @Column(nullable = false) private Boolean finished;

    @Column(nullable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;

    @PrePersist void onCreate(){
        if (createdAt==null) createdAt = LocalDateTime.now();
        if (updatedAt==null) updatedAt = LocalDateTime.now();
        if (finished==null) finished=false;
        if (step==null) step="MCQ";
    }
    @PreUpdate void onUpdate(){ updatedAt = LocalDateTime.now(); }
}
