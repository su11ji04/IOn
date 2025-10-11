package capstone.workbook.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WorkbookListResponse {
    private Long workbook_id;
    private String activityTitle;
}
