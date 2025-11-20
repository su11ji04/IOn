package capstone.workbook.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimulationPythonResponseDto {
    private String chapterTitle;
    private String lessonTitle;

    private String simulationSituationExplain;
    private String aiLine;
}
