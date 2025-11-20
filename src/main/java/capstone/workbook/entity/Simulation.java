package capstone.workbook.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "simulation",
        indexes = {
                @Index(name = "idx_simulation_user", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Simulation {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "simulation_id")
        private int simulationId;
        @Column(name = "done")
        private int done; // 0 -> 시작 전, 1 -> 완료

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

        @Lob
        @Column(name = "simulation_situation_explain", columnDefinition = "TEXT")
        private String simulationSituationExplain;

        @Builder.Default
        @ElementCollection
        @CollectionTable(
                name = "dialogues_group",
                joinColumns = @JoinColumn(name = "simulation_id")
        )
        @OrderColumn(name = "seq")
        private List<Dialogue> dialogues = new ArrayList<>();

        @Lob
        @Column(name = "simulation_feedback" , columnDefinition = "TEXT")
        private String simulationFeedback;
}
