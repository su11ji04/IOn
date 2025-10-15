package capstone.workbook.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class WorkbookAnswerRequest {
    @JsonProperty("optionAnswer")
    private String optionAnswer;

    @JsonProperty("writingAnswer")
    private String writingAnswer;

    @JsonProperty("simAnswer")
    private String simAnswer;

    private Boolean finished;
}
