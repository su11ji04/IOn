package capstone.workbook.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkbookListResponse {
    private String chapterTitle;
    private int chapterProgress;

    private List<LessonDto> lessons;

    private int totalPages;
    private int size;
}