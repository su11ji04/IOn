package capstone.workbook.entity;

import capstone.workbook.dto.ActivityType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "workbook_step_answer")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkbookStepAnswer {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long workbookId;
    private Integer stepIndex;

    @Enumerated(EnumType.STRING)
    private ActivityType stepType;   // MCQ / WRITING / SIMULATION

    @Lob
    @Column(columnDefinition = "CLOB")
    private String answerText;       // MCQ: 사용자가 선택한 옵션, WRITING: 서술, SIM: 히스토리 요약(json 가능)

    private Boolean mcqCorrect;      // MCQ일 때만 사용 (그 외 null)

    private LocalDateTime createdAt;
}
