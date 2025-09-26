package capstone.workbook.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class WorkbookFeedbackResponse {

    // FastAPI: "overall_comment" → Java: overallComment
    @JsonProperty("overall_comment")
    private String overallComment;

    // FastAPI: tips는 배열(List<String>)
    private List<String> tips;
}
