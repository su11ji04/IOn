package capstone.workbook.dto;

import capstone.workbook.entity.Dialogue;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimulationDto {
    private Integer simulationId;
    private Integer done; // 0 -> 시작 전, 1 -> 완료

    private Integer userId;

    private Integer chapterId;
    private String chapterTitle;
    private Integer lessonId;
    private String lessonTitle;

    private String simulationSituationExplain;
    private List<Dialogue> dialogues;
}
