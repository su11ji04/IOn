package capstone.workbook.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "workbook",
        indexes = {
                @Index(name = "idx_workbook_user", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Workbook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "workbook_id")
    private int workbookId;
    @Column(name = "done")
    private int done; // 0->시작 전 1->워크북 활동 완료

    @Column(name = "user_id")
    private int userId;

    @Column(name = "chapter_id")
    private int chapterId;
    @Column(name = "chapter_title")
    private String chapterTitle;
    @Column(name = "lesson_id")
    private int lessonId;
    @Column(name = "lesson_title")
    private String lessonTitle;

    // ---------------- 작성형 ----------------
    @Lob
    @Column(name = "descriptive_form_question", columnDefinition = "TEXT")
    private String descriptiveFormQuestion;
    @Lob
    @Column(name = "descriptive_form_answer", columnDefinition = "TEXT")
    private String descriptiveFormAnswer;
    @Lob
    @Column(name = "descriptive_form_example", columnDefinition = "TEXT")
    private String descriptiveFormExample;

    // ---------------- 선택형 ----------------
    @Lob
    @Column(name = "selective_question", columnDefinition = "TEXT")
    private String selectiveQuestion;
    @Lob
    @Column(name = "selective_options", columnDefinition = "TEXT")
    private String selectiveOptions;
    @Lob
    @Column(name = "selective_answer", columnDefinition = "TEXT")
    private String selectiveAnswer;
    @Lob
    @Column(name = "selective_example", columnDefinition = "TEXT")
    private String selectiveExample;

    // ---------------- 피드백 ----------------
    @Lob
    @Column(name = "workbook_feedback", columnDefinition = "TEXT")
    private String workbookFeedback;
}
