package capstone.workbook.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter @Setter
public class WorkbookActivity {
    private String type;                   // "작성형" / "선택형" / "시뮬레이션"
    private String instruction;

    // 작성형
    @JsonProperty("example_answer")
    private String exampleAnswer;

    // 선택형
    private List<String> options;
    @JsonProperty("optimal_option")
    private String optimalOption;

    // 시뮬레이션
    private String situation;
    @JsonProperty("your_response")
    private String yourResponse;
    @JsonProperty("ai_optimal_response")
    private String aiOptimalResponse;
}
