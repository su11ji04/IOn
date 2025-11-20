package capstone.workbook.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "workbook_theory")
@Getter
@NoArgsConstructor
public class WorkbookTheory {
    @Id
    @Column(name = "chapter_id")
    private Integer chapterId;

    @Lob
    @Column(name = "necessity")
    private String necessity;
    @Lob
    @Column(name = "study_goal")
    private String studyGoal;
    @Lob
    @Column(name = "notion")
    private String notion;
}
