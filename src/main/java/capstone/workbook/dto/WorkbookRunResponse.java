package capstone.workbook.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkbookRunResponse {
    @JsonProperty("chapter_id")
    private int chapterId;

    @JsonProperty("workbook_id")
    private Long workbookId;

    @JsonProperty("activity_title")
    private String activityTitle;

    private List<WorkbookActivity> activities;
}
