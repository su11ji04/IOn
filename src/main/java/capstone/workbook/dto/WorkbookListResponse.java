package capstone.workbook.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WorkbookListResponse {
    private int chapter_id;
    private Long workbook_id;
    private String activityTitle;
}
