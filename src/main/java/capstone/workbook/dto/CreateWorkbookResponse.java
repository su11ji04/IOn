package capstone.workbook.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateWorkbookResponse {
    @JsonProperty("activity_title")
    private String activityTitle;

    private List<WorkbookActivity> activities;
}
