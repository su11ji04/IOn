package capstone.workbook.dto;

import jakarta.persistence.Column;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TheoryDto {
    private String chapterTitle;

    private String necessity;
    private String studyGoal;
    private String notion;
}
