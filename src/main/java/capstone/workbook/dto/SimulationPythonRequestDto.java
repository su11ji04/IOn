package capstone.workbook.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimulationPythonRequestDto {
    private int chapterId;
    private int lessonId;
    private List<DialogueDto> dialogues;
}
