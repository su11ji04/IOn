package capstone.workbook.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;


@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter @Setter
public class WorkbookFeedbackDto {
    @JsonProperty("workbook_feedback")
    private String workbookFeedback;
}
